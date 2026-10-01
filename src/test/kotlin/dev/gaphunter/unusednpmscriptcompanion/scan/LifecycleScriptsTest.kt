package dev.gaphunter.unusednpmscriptcompanion.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifecycleScriptsTest {

    @Test
    fun `pretest is always used even with no siblings`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("pretest", setOf("pretest")))
    }

    @Test
    fun `test is always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("test", setOf("test")))
    }

    @Test
    fun `posttest is always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("posttest", setOf("posttest")))
    }

    @Test
    fun `prepare is always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("prepare", setOf("prepare")))
    }

    @Test
    fun `prepublishOnly is always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("prepublishOnly", setOf("prepublishOnly")))
    }

    @Test
    fun `preinstall and postinstall are always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("preinstall", setOf("preinstall")))
        assertTrue(LifecycleScripts.isAlwaysUsed("postinstall", setOf("postinstall")))
    }

    @Test
    fun `preversion and postversion are always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("preversion", setOf("preversion")))
        assertTrue(LifecycleScripts.isAlwaysUsed("postversion", setOf("postversion")))
    }

    @Test
    fun `start is always used even with zero text references`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("start", emptySet()))
    }

    @Test
    fun `prebuild is used when a real build script exists`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("prebuild", setOf("prebuild", "build")))
    }

    @Test
    fun `postbuild is used when a real build script exists`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("postbuild", setOf("postbuild", "build")))
    }

    @Test
    fun `prefoo is not a lifecycle hook when no foo script exists`() {
        assertFalse(LifecycleScripts.isAlwaysUsed("prefoo", setOf("prefoo")))
    }

    @Test
    fun `an ordinary script name is not always used`() {
        assertFalse(LifecycleScripts.isAlwaysUsed("lint", setOf("lint", "build")))
    }

    // Regression (2026-10-01): `npm stop` / `npm restart` run these by name.
    @Test
    fun `stop and restart are always used`() {
        assertTrue(LifecycleScripts.isAlwaysUsed("stop", setOf("stop")))
        assertTrue(LifecycleScripts.isAlwaysUsed("restart", setOf("restart")))
        assertTrue(LifecycleScripts.isAlwaysUsed("prestop", setOf("stop", "prestop")))
    }
}
