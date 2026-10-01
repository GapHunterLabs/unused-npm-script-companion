package dev.gaphunter.unusednpmscriptcompanion.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptReferenceMatcherTest {

    @Test
    fun `matches npm run form`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("npm run clean && npm run compile", "clean"))
    }

    @Test
    fun `matches yarn shorthand form`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("run: yarn lint", "lint"))
    }

    @Test
    fun `matches pnpm run form`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("script: pnpm run build", "build"))
    }

    @Test
    fun `matches pnpm shorthand form`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("script: pnpm build", "build"))
    }

    @Test
    fun `does not match a bare word with no runner keyword`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("Run the build step first, then deploy.", "build"))
    }

    @Test
    fun `does not match as substring of a longer script name`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("npm run build-prod", "build"))
    }

    @Test
    fun `does not match when script name is a substring the other way`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("npm run build", "build-prod"))
    }

    @Test
    fun `does not match rebuild as build`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("npm run rebuild", "build"))
    }

    @Test
    fun `no match at all when text has nothing relevant`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("Some unrelated README prose.", "build"))
    }

    // Regression (2026-10-01): these real invocation forms were not
    // recognized, so the script was reported as "possibly unused".
    @Test
    fun `matches yarn run form`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("      - run: yarn run e2e", "e2e"))
    }

    @Test
    fun `matches npm run-script and bun forms`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("npm run-script lint", "lint"))
        assertTrue(ScriptReferenceMatcher.isReferenced("bun run dev", "dev"))
        assertTrue(ScriptReferenceMatcher.isReferenced("bun dev", "dev"))
    }

    @Test
    fun `matches with options between the runner and the name`() {
        assertTrue(ScriptReferenceMatcher.isReferenced("npm run -s build", "build"))
        assertTrue(ScriptReferenceMatcher.isReferenced("npm run --if-present lint", "lint"))
        assertTrue(ScriptReferenceMatcher.isReferenced("yarn --silent run test:unit", "test:unit"))
    }

    @Test
    fun `matches npm-run-all arguments, exact and globs`() {
        val scripts = "\"build\": \"run-s clean compile\", \"check\": \"npm-run-all --parallel lint:* test\""
        assertTrue(ScriptReferenceMatcher.isReferenced(scripts, "clean"))
        assertTrue(ScriptReferenceMatcher.isReferenced(scripts, "compile"))
        assertTrue(ScriptReferenceMatcher.isReferenced(scripts, "lint:js"))
        assertTrue(ScriptReferenceMatcher.isReferenced(scripts, "test"))
        assertFalse(ScriptReferenceMatcher.isReferenced(scripts, "lint"))
        assertFalse(ScriptReferenceMatcher.isReferenced(scripts, "lint:js:fix"))
        assertTrue(ScriptReferenceMatcher.isReferenced("run-p build:**", "build:web:prod"))
    }

    @Test
    fun `npm-run-all arguments stop at the end of the shell command`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("\"all\": \"run-s clean && deploy\"", "deploy"))
        assertFalse(ScriptReferenceMatcher.isReferenced("\"all\": \"run-p lint\", \"other\": \"echo deploy\"", "deploy"))
    }

    @Test
    fun `a word that only contains run-s is not the npm-run-all CLI`() {
        assertFalse(ScriptReferenceMatcher.isReferenced("rerun-s clean", "clean"))
    }
}
