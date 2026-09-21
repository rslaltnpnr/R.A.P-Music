package com.ozin.music

import com.ozin.music.core.domain.QueryIntentParser
import com.ozin.music.core.domain.SmartRuleField
import com.ozin.music.core.domain.SmartRuleOperator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryIntentParserTest {

    @Test
    fun `favorite keyword maps to a favorite rule`() {
        val intent = QueryIntentParser.parse("favori şarkılarım")
        assertTrue(intent.rules.any { it.field == SmartRuleField.FAVORITE && it.value == "true" })
    }

    @Test
    fun `english favorite keyword also matches`() {
        val intent = QueryIntentParser.parse("show my favorite tracks")
        assertTrue(intent.rules.any { it.field == SmartRuleField.FAVORITE })
    }

    @Test
    fun `most played phrase sets the sort hint and adds no filter rule`() {
        val intent = QueryIntentParser.parse("en çok dinlenen şarkılar")
        assertTrue(intent.sortByPlayCountDescending)
    }

    @Test
    fun `last 30 days phrase reuses the recently-added rule shape`() {
        val intent = QueryIntentParser.parse("son 30 gün eklenenler")
        assertTrue(
            intent.rules.any {
                it.field == SmartRuleField.DATE_ADDED && it.operator == SmartRuleOperator.GREATER_OR_EQUAL && it.value == "30"
            }
        )
    }

    @Test
    fun `energetic mood keyword maps to a mood tag rule`() {
        val intent = QueryIntentParser.parse("enerjik müzik istiyorum")
        assertTrue(
            intent.rules.any {
                it.field == SmartRuleField.MOOD_TAG && it.operator == SmartRuleOperator.CONTAINS && it.value == "ENERGETIC"
            }
        )
    }

    @Test
    fun `night mood keyword in english maps to night mood rule`() {
        val intent = QueryIntentParser.parse("night music")
        assertTrue(intent.rules.any { it.field == SmartRuleField.MOOD_TAG && it.value == "NIGHT" })
    }

    @Test
    fun `unrecognized free text becomes the remainder rather than a rule`() {
        val intent = QueryIntentParser.parse("Queen")
        assertEquals(emptyList<Any>(), intent.rules)
        assertEquals("queen", intent.remainderText)
    }

    @Test
    fun `blank query yields no rules and no remainder`() {
        val intent = QueryIntentParser.parse("   ")
        assertTrue(intent.rules.isEmpty())
        assertEquals("", intent.remainderText)
    }

    @Test
    fun `multiple recognized phrases combine into multiple rules`() {
        val intent = QueryIntentParser.parse("favori ve enerjik şarkılar")
        assertTrue(intent.rules.any { it.field == SmartRuleField.FAVORITE })
        assertTrue(intent.rules.any { it.field == SmartRuleField.MOOD_TAG && it.value == "ENERGETIC" })
    }
}
