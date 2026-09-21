package com.ozin.music.core.remote.webdav

import com.ozin.music.core.remote.AudioFileTypes
import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteEntry
import com.ozin.music.core.remote.RemoteFolder
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import java.net.URLDecoder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pure XML parsing of a WebDAV PROPFIND multistatus response. Deliberately
 * free of Android/OkHttp dependencies so it is directly unit-testable on the
 * JVM with a fixture string.
 */
object WebDavXmlParser {

    fun parseMultistatus(xml: String, requestPath: String): List<RemoteEntry> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val responses = document.getElementsByTagNameNS("*", "response")
        val normalizedRequestPath = normalize(requestPath)

        val entries = mutableListOf<RemoteEntry>()
        for (i in 0 until responses.length) {
            val responseEl = responses.item(i) as? Element ?: continue
            val href = firstChildText(responseEl, "href") ?: continue
            val decodedPath = normalize(decodeHrefPath(href))
            if (decodedPath == normalizedRequestPath) continue

            var isCollection = false
            var displayName: String? = null
            val propStats = responseEl.getElementsByTagNameNS("*", "propstat")
            for (j in 0 until propStats.length) {
                val propStat = propStats.item(j) as? Element ?: continue
                val status = firstChildText(propStat, "status").orEmpty()
                if (!status.contains("200")) continue
                val prop = firstChildElement(propStat, "prop") ?: continue
                val resourceType = firstChildElement(prop, "resourcetype")
                if (resourceType != null && resourceType.getElementsByTagNameNS("*", "collection").length > 0) {
                    isCollection = true
                }
                firstChildText(prop, "displayname")?.let { displayName = it }
            }

            val name = displayName?.takeIf { it.isNotBlank() }
                ?: decodedPath.trimEnd('/').substringAfterLast('/').ifBlank { decodedPath }

            entries += if (isCollection) {
                RemoteFolder(name = name, path = decodedPath)
            } else if (AudioFileTypes.isAudioFile(name)) {
                RemoteAudioFile(name = name, path = decodedPath, sizeBytes = null)
            } else {
                continue
            }
        }
        return entries
    }

    private fun decodeHrefPath(href: String): String {
        val rawPath = if (href.startsWith("http://") || href.startsWith("https://")) {
            runCatching { URI(href).rawPath }.getOrNull() ?: href
        } else {
            href
        }
        return runCatching { URLDecoder.decode(rawPath, "UTF-8") }.getOrDefault(rawPath)
    }

    private fun normalize(path: String): String {
        var p = path
        if (!p.startsWith("/")) p = "/$p"
        if (p.length > 1 && p.endsWith("/")) p = p.dropLast(1)
        return p
    }

    private fun firstChildText(parent: Element, localName: String): String? =
        firstChildElement(parent, localName)?.textContent?.trim()

    private fun firstChildElement(parent: Element, localName: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && node.localName == localName) return node
        }
        return null
    }
}
