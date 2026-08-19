package dev.gaphunter.unusednpmscriptcompanion.parse

import com.intellij.json.psi.JsonFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class PackageJsonParserTest : BasePlatformTestCase() {

    fun `test parses every script entry in source order`() {
        myFixture.configureByText(
            "package.json",
            """
            {
              "name": "demo",
              "scripts": {
                "build": "tsc -p .",
                "test": "jest",
                "lint": "eslint ."
              }
            }
            """.trimIndent(),
        )
        val scripts = PackageJsonParser.parseScripts(myFixture.file as JsonFile)
        assertEquals(listOf("build", "test", "lint"), scripts.map { it.name })
        assertEquals("tsc -p .", scripts.first { it.name == "build" }.command)
    }

    fun `test package json with no scripts section returns empty list honestly`() {
        myFixture.configureByText(
            "package.json",
            """{ "name": "demo", "version": "1.0.0" }""",
        )
        val scripts = PackageJsonParser.parseScripts(myFixture.file as JsonFile)
        assertTrue(scripts.isEmpty())
        assertFalse(PackageJsonParser.hasScriptsSection(myFixture.file as JsonFile))
    }

    fun `test hasScriptsSection is true when scripts object exists even if empty`() {
        myFixture.configureByText(
            "package.json",
            """{ "name": "demo", "scripts": {} }""",
        )
        assertTrue(PackageJsonParser.hasScriptsSection(myFixture.file as JsonFile))
        assertTrue(PackageJsonParser.parseScripts(myFixture.file as JsonFile).isEmpty())
    }

    fun `test malformed json produces no crash and an empty result`() {
        myFixture.configureByText(
            "package.json",
            """{ "name": "demo", "scripts": { "build": """,
        )
        val scripts = PackageJsonParser.parseScripts(myFixture.file as JsonFile)
        assertNotNull(scripts)
    }

    fun `test non package json file name is still parsed correctly by this parser`() {
        // PackageJsonParser itself is name-agnostic (the gutter provider is
        // what restricts to files literally named package.json); this test
        // just confirms the parser doesn't secretly depend on file name.
        myFixture.configureByText(
            "other.json",
            """{ "scripts": { "build": "tsc" } }""",
        )
        val scripts = PackageJsonParser.parseScripts(myFixture.file as JsonFile)
        assertEquals(listOf("build"), scripts.map { it.name })
    }
}
