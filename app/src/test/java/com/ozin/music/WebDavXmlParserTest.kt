package com.ozin.music

import com.ozin.music.core.remote.RemoteAudioFile
import com.ozin.music.core.remote.RemoteFolder
import com.ozin.music.core.remote.webdav.WebDavXmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavXmlParserTest {

    private val sampleResponse = """
        <?xml version="1.0" encoding="utf-8"?>
        <D:multistatus xmlns:D="DAV:">
          <D:response>
            <D:href>/music/</D:href>
            <D:propstat>
              <D:prop>
                <D:displayname>music</D:displayname>
                <D:resourcetype><D:collection/></D:resourcetype>
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
          <D:response>
            <D:href>/music/Rock/</D:href>
            <D:propstat>
              <D:prop>
                <D:displayname>Rock</D:displayname>
                <D:resourcetype><D:collection/></D:resourcetype>
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
          <D:response>
            <D:href>/music/song%201.mp3</D:href>
            <D:propstat>
              <D:prop>
                <D:displayname>song 1.mp3</D:displayname>
                <D:resourcetype/>
                <D:getcontentlength>4200000</D:getcontentlength>
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
          <D:response>
            <D:href>/music/readme.txt</D:href>
            <D:propstat>
              <D:prop>
                <D:displayname>readme.txt</D:displayname>
                <D:resourcetype/>
              </D:prop>
              <D:status>HTTP/1.1 200 OK</D:status>
            </D:propstat>
          </D:response>
        </D:multistatus>
    """.trimIndent()

    @Test
    fun `parses folders and audio files, skips self and non-audio entries`() {
        val entries = WebDavXmlParser.parseMultistatus(sampleResponse, "/music/")

        assertEquals(2, entries.size)

        val folder = entries.filterIsInstance<RemoteFolder>().single()
        assertEquals("Rock", folder.name)
        assertEquals("/music/Rock", folder.path)

        val audio = entries.filterIsInstance<RemoteAudioFile>().single()
        assertEquals("song 1.mp3", audio.name)
        assertEquals("/music/song 1.mp3", audio.path)
    }

    @Test
    fun `an empty non-collection response with only the self entry yields nothing`() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:multistatus xmlns:D="DAV:">
              <D:response>
                <D:href>/empty/</D:href>
                <D:propstat>
                  <D:prop>
                    <D:displayname>empty</D:displayname>
                    <D:resourcetype><D:collection/></D:resourcetype>
                  </D:prop>
                  <D:status>HTTP/1.1 200 OK</D:status>
                </D:propstat>
              </D:response>
            </D:multistatus>
        """.trimIndent()

        val entries = WebDavXmlParser.parseMultistatus(xml, "/empty")
        assertTrue(entries.isEmpty())
    }
}
