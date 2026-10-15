package dev.sk2andy.materialbrowser.browser.gecko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeckoAuthPromptRealmTest {
    @Test
    fun `english message gives the quoted realm only`() {
        assertEquals(
            "Candy Test",
            GeckoAuthPromptRealm.fromMessage(
                "http://127.0.0.1:34143 is requesting your username and password. " +
                    "The site says: “Candy Test”",
            ),
        )
    }

    @Test
    fun `proxy message gives the quoted realm`() {
        assertEquals(
            "Corporate proxy",
            GeckoAuthPromptRealm.fromMessage(
                "The proxy moz-proxy://10.0.0.1:3128 is requesting a username and password. " +
                    "The site says: “Corporate proxy”",
            ),
        )
    }

    @Test
    fun `guillemets and low-high quotes are read`() {
        assertEquals(
            "Тест",
            GeckoAuthPromptRealm.fromMessage(
                "http://example.ru запрашивает имя пользователя и пароль. Сайт сообщает: «Тест»",
            ),
        )
        assertEquals(
            "Bereich",
            GeckoAuthPromptRealm.fromMessage("http://example.de verlangt: „Bereich“"),
        )
    }

    @Test
    fun `quotes inside the realm stay part of it`() {
        assertEquals(
            "the “inner” zone",
            GeckoAuthPromptRealm.fromMessage("http://h is asking. The site says: “the “inner” zone”."),
        )
    }

    @Test
    fun `message without a realm gives none`() {
        assertNull(
            GeckoAuthPromptRealm.fromMessage(
                "http://127.0.0.1:34143 is requesting your username and password.",
            ),
        )
        assertNull(GeckoAuthPromptRealm.fromMessage("The site says: “”"))
        assertNull(GeckoAuthPromptRealm.fromMessage("ends with a lone quote\""))
        assertNull(GeckoAuthPromptRealm.fromMessage(null))
        assertNull(GeckoAuthPromptRealm.fromMessage("   "))
    }
}
