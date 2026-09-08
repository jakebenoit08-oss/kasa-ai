package com.example

import com.example.core.ai.SupportedLanguage
import com.example.core.ai.context.GhanaianContextConfig
import com.example.core.ai.context.LanguageCapabilityStatus
import com.example.core.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KasaGhanaianIntelligenceUnitTest {

  @Test
  fun `test 1 - english and ghanaian context in centralized instructions`() {
    val sysInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.ENGLISH)
    assertNotNull(sysInstruction)

    // Verify Cultural Grounding
    assertTrue(sysInstruction.contains("Accra"))
    assertTrue(sysInstruction.contains("Kumasi"))
    assertTrue(sysInstruction.contains("Jollof"))
    assertTrue(sysInstruction.contains("Waakye"))
    assertTrue(sysInstruction.contains("Banku"))
    assertTrue(sysInstruction.contains("Fufu"))
    assertTrue(sysInstruction.contains("Trotro"))
    assertTrue(sysInstruction.contains("Mobile Money"))
  }

  @Test
  fun `test 2 - pidgin tone mirroring directive`() {
    val sysInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.ENGLISH)
    assertTrue(sysInstruction.contains("Ghanaian Pidgin"))
    assertTrue(sysInstruction.contains("without cartoonish or forced exaggeration"))
    assertTrue(sysInstruction.contains("CRITICAL BALANCE RULE: NEVER force words like"))
  }

  @Test
  fun `test 3 - twi and multilingual support guidelines`() {
    val twiInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.TWI)
    assertTrue(twiInstruction.contains("Akan / Twi"))
    assertTrue(twiInstruction.contains("User Language Preference: The user has set their preferred language preference to Akan / Twi"))
  }

  @Test
  fun `test 4 - ghanaian currency and cedi verification`() {
    val sysInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.ENGLISH)
    assertTrue(sysInstruction.contains("Cedis"))
    assertTrue(sysInstruction.contains("GH₵"))
    assertTrue(sysInstruction.contains("Pesewas"))
  }

  @Test
  fun `test 5 - ghanaian education terminology and context`() {
    val sysInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.ENGLISH)
    assertTrue(sysInstruction.contains("WASSCE"))
    assertTrue(sysInstruction.contains("BECE"))
    assertTrue(sysInstruction.contains("SHS"))
    assertTrue(sysInstruction.contains("JHS"))
    assertTrue(sysInstruction.contains("Core Maths"))
    assertTrue(sysInstruction.contains("Integrated Science"))
  }

  @Test
  fun `test 6 - zero hallucination live market and exchange rate safety boundary`() {
    val sysInstruction = GhanaianContextConfig.buildSystemInstruction(SupportedLanguage.ENGLISH)
    assertTrue(sysInstruction.contains("LIVE/CURRENT exchange rates"))
    assertTrue(sysInstruction.contains("official banking or news source"))
    assertTrue(sysInstruction.contains("Never invent fake words"))
  }

  @Test
  fun `test 7 - live voice system instruction consistency`() {
    val liveInstruction = GhanaianContextConfig.buildLiveSystemInstruction(SupportedLanguage.ENGLISH)
    assertTrue(liveInstruction.contains("KASA Live mode"))
    assertTrue(liveInstruction.contains("NEVER output markdown symbols"))
    assertTrue(liveInstruction.contains("Deeply understand Ghanaian places"))
    assertTrue(liveInstruction.contains("live real-time financial/news data requires checking a current verified source"))
  }

  @Test
  fun `test 8 - language capability matrix and honesty verification`() {
    val details = GhanaianContextConfig.LANGUAGE_SUPPORT_DETAILS
    assertEquals(7, details.size)

    val english = details.first { it.language == SupportedLanguage.ENGLISH }
    assertEquals(LanguageCapabilityStatus.STRONG_SUPPORT, english.status)

    val twi = details.first { it.language == SupportedLanguage.TWI }
    assertEquals(LanguageCapabilityStatus.PARTIAL_SUPPORT, twi.status)

    val ga = details.first { it.language == SupportedLanguage.GA }
    assertEquals(LanguageCapabilityStatus.EXPERIMENTAL, ga.status)

    val ewe = details.first { it.language == SupportedLanguage.EWE }
    assertEquals(LanguageCapabilityStatus.EXPERIMENTAL, ewe.status)
  }
}
