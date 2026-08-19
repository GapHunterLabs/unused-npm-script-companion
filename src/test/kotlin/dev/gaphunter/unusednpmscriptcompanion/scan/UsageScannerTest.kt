package dev.gaphunter.unusednpmscriptcompanion.scan

import dev.gaphunter.unusednpmscriptcompanion.model.NpmScript
import dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageScannerTest {

    private fun script(name: String, command: String = "echo $name") = NpmScript(name, command, 0)

    @Test
    fun `script called from another script in the same package json is used`() {
        val build = script("build", "npm run clean && npm run compile")
        val clean = script("clean")
        val compile = script("compile")

        val usages = UsageScanner.scan(listOf(build, clean, compile), emptyMap(), null)

        assertEquals(UsageVerdict.USED_IN_OWN_PACKAGE_JSON, usages.first { it.script.name == "clean" }.verdict)
        assertEquals(UsageVerdict.USED_IN_OWN_PACKAGE_JSON, usages.first { it.script.name == "compile" }.verdict)
    }

    @Test
    fun `script referenced in a github actions workflow is used`() {
        val test = script("test:ci")
        val usages = UsageScanner.scan(
            listOf(test),
            mapOf(".github/workflows/ci.yml" to "run: npm run test:ci"),
            null,
        )
        assertEquals(UsageVerdict.USED_IN_CI, usages.single().verdict)
    }

    @Test
    fun `script referenced in gitlab ci is used`() {
        val deploy = script("deploy")
        val usages = UsageScanner.scan(
            listOf(deploy),
            mapOf(".gitlab-ci.yml" to "script:\n  - npm run deploy"),
            null,
        )
        assertEquals(UsageVerdict.USED_IN_CI, usages.single().verdict)
    }

    @Test
    fun `script mentioned in readme is used`() {
        val docs = script("docs")
        val usages = UsageScanner.scan(
            listOf(docs),
            emptyMap(),
            "Run `npm run docs` to generate the documentation site.",
        )
        assertEquals(UsageVerdict.USED_IN_README, usages.single().verdict)
    }

    @Test
    fun `genuinely orphaned script is flagged`() {
        val forgotten = script("forgotten-migration")
        val usages = UsageScanner.scan(
            listOf(forgotten),
            mapOf(".github/workflows/ci.yml" to "run: npm run build"),
            "Run `npm run build` to compile.",
        )
        assertEquals(UsageVerdict.ORPHANED, usages.single().verdict)
    }

    @Test
    fun `npm lifecycle script is never flagged orphaned even with zero references`() {
        val pretest = script("pretest")
        val usages = UsageScanner.scan(listOf(pretest), emptyMap(), null)
        assertEquals(UsageVerdict.LIFECYCLE_HOOK, usages.single().verdict)
    }

    @Test
    fun `pre hook of a real sibling script is never flagged orphaned`() {
        val build = script("build")
        val prebuild = script("prebuild")
        val usages = UsageScanner.scan(listOf(build, prebuild), emptyMap(), null)
        assertEquals(UsageVerdict.LIFECYCLE_HOOK, usages.first { it.script.name == "prebuild" }.verdict)
    }

    @Test
    fun `substring mention in readme does not count as a reference`() {
        val build = script("build")
        val usages = UsageScanner.scan(
            listOf(build),
            emptyMap(),
            "This project uses a custom build pipeline written in Rust.",
        )
        assertEquals(UsageVerdict.ORPHANED, usages.single().verdict)
    }

    @Test
    fun `own package json reference takes priority over ci and readme`() {
        val build = script("build")
        val ci = script("ci", "npm run build")
        val usages = UsageScanner.scan(
            listOf(build, ci),
            mapOf(".github/workflows/ci.yml" to "run: npm run build"),
            "See npm run build in the docs.",
        )
        val buildUsage = usages.first { it.script.name == "build" }
        assertEquals(UsageVerdict.USED_IN_OWN_PACKAGE_JSON, buildUsage.verdict)
    }
}
