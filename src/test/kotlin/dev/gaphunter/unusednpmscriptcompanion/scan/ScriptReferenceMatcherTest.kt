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
}
