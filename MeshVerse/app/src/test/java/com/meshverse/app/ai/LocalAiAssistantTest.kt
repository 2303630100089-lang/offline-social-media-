package com.meshverse.app.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAiAssistantTest {

    @Test
    fun `routeCommand returns SEND_SOS for SOS prompts`() {
        val byPrefix = LocalAiAssistant.routeCommand("  SOS help now")
        val byPhrase = LocalAiAssistant.routeCommand("please send sos to nearby peers")

        assertEquals(LocalAiAssistant.Action.SEND_SOS, byPrefix.action)
        assertEquals(LocalAiAssistant.Action.SEND_SOS, byPhrase.action)
        assertEquals("  SOS help now", byPrefix.message)
    }

    @Test
    fun `routeCommand routes hospital requests`() {
        val result = LocalAiAssistant.routeCommand("Find nearest hospital")

        assertEquals(LocalAiAssistant.Action.FIND_HOSPITAL, result.action)
    }

    @Test
    fun `routeCommand routes walkie talkie requests`() {
        val byKeyword = LocalAiAssistant.routeCommand("open walkie room")
        val byPhrase = LocalAiAssistant.routeCommand("start push to talk mode")

        assertEquals(LocalAiAssistant.Action.START_WALKIE_TALKIE, byKeyword.action)
        assertEquals(LocalAiAssistant.Action.START_WALKIE_TALKIE, byPhrase.action)
    }

    @Test
    fun `routeCommand routes marketplace requests`() {
        val result = LocalAiAssistant.routeCommand("open marketplace")

        assertEquals(LocalAiAssistant.Action.OPEN_MARKETPLACE, result.action)
    }

    @Test
    fun `routeCommand returns unknown for unsupported prompts`() {
        val result = LocalAiAssistant.routeCommand("show my recent notes")

        assertEquals(LocalAiAssistant.Action.UNKNOWN, result.action)
    }
}
