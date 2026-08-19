package dev.gaphunter.unusednpmscriptcompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * End-to-end: real PSI + real daemon slow-line-marker pass (confirmed
 * static signature via `javap` against the pinned platform jar --
 * `DaemonCodeAnalyzerImpl.getLineMarkers(Document, Project)`), not a
 * direct unit call into [UnusedNpmScriptLineMarkerProvider]'s private
 * methods -- this is the only way to confirm the provider is actually
 * *wired up* to fire for `package.json` files, not just that its
 * internal logic is correct in isolation (that part is already covered
 * exhaustively by [dev.gaphunter.unusednpmscriptcompanion.scan.UsageScannerTest]).
 */
class UnusedNpmScriptLineMarkerProviderTest : BasePlatformTestCase() {

    private fun collectMarkers(): List<LineMarkerInfo<*>> {
        myFixture.doHighlighting()
        return DaemonCodeAnalyzerImpl.getLineMarkers(myFixture.editor.document, project)
    }

    private fun tooltipFor(scriptName: String): String {
        val markers = collectMarkers()
        val marker = markers.first { it.element?.text == "\"$scriptName\"" }
        return marker.lineMarkerTooltip ?: ""
    }

    fun `test used script gets the used icon`() {
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "build": "tsc -p .",
                "ci": "npm run build"
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"build\"" }
        assertSame(UnusedNpmScriptIcons.USED, marker.icon)
    }

    fun `test orphaned script gets the orphaned icon`() {
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "forgotten-migration": "node scripts/migrate-legacy.js"
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"forgotten-migration\"" }
        assertSame(UnusedNpmScriptIcons.ORPHANED, marker.icon)
    }

    fun `test lifecycle script never gets the orphaned icon`() {
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "pretest": "eslint .",
                "test": "jest"
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"pretest\"" }
        assertSame(UnusedNpmScriptIcons.USED, marker.icon)
    }

    fun `test script referenced from a github actions workflow is used`() {
        myFixture.addFileToProject(
            ".github/workflows/ci.yml",
            """
            jobs:
              build:
                steps:
                  - run: npm run build
            """.trimIndent(),
        )
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "build": "tsc -p ."
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"build\"" }
        assertSame(UnusedNpmScriptIcons.USED, marker.icon)
        assertTrue(tooltipFor("build").contains(".github/workflows/ci.yml"))
    }

    fun `test script referenced from gitlab ci is used`() {
        myFixture.addFileToProject(
            ".gitlab-ci.yml",
            """
            deploy:
              script:
                - npm run deploy
            """.trimIndent(),
        )
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "deploy": "node deploy.js"
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"deploy\"" }
        assertSame(UnusedNpmScriptIcons.USED, marker.icon)
    }

    fun `test script mentioned in readme is used`() {
        myFixture.addFileToProject(
            "README.md",
            "Run `npm run docs` to build the documentation site.",
        )
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "docs": "typedoc ."
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"docs\"" }
        assertSame(UnusedNpmScriptIcons.USED, marker.icon)
    }

    fun `test substring mention in readme does not prevent an orphan flag`() {
        myFixture.addFileToProject(
            "README.md",
            "This project uses a custom build pipeline written in Rust.",
        )
        myFixture.configureByText(
            "package.json",
            """
            {
              "scripts": {
                "build": "tsc -p ."
              }
            }
            """.trimIndent(),
        )
        val marker = collectMarkers().first { it.element?.text == "\"build\"" }
        assertSame(UnusedNpmScriptIcons.ORPHANED, marker.icon)
    }

    fun `test package json with no scripts section produces no markers and no crash`() {
        myFixture.configureByText(
            "package.json",
            """{ "name": "demo", "version": "1.0.0" }""",
        )
        val markers = collectMarkers()
        assertTrue(markers.isEmpty())
    }

    fun `test a json file that is not named package json is ignored`() {
        myFixture.configureByText(
            "other.json",
            """
            {
              "scripts": {
                "build": "tsc -p ."
              }
            }
            """.trimIndent(),
        )
        val markers = collectMarkers()
        assertTrue(markers.isEmpty())
    }
}
