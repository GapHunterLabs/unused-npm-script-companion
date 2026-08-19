package dev.gaphunter.unusednpmscriptcompanion.scan

/**
 * npm script names that npm itself invokes automatically at specific
 * points in its own lifecycle -- never real orphans even when no text
 * reference to them exists anywhere in the project, because their
 * "caller" is the npm CLI itself, not project source. Curated directly
 * from npm's own documented lifecycle events (`prepare`, `prepublish`
 * et al. under `npm-install`/`npm-publish`/`npm-version`/`npm-pack`;
 * `pretest`/`posttest` under `npm-test`), same "documented, not
 * guessed" discipline as every other allowlist in this catalog
 * (`GradleBuildFileParser`'s configuration list,
 * `ExpensiveRunPatterns`).
 */
object LifecycleScripts {

    /**
     * Exact names npm runs by convention regardless of any text
     * reference. `start` is included even though it isn't a pre/post
     * hook of anything else -- it's the universal "how do I run this
     * package" entry point (`npm start`), so flagging it as orphaned
     * because nothing *calls* it by name would be actively wrong: a
     * human runs it directly from the command line, which this plugin
     * (static text analysis only) has no way to observe.
     */
    private val ALWAYS_USED: Set<String> = setOf(
        "start",
        "pretest", "test", "posttest",
        "prepublish", "prepare", "prepublishOnly", "prepack", "postpack", "publish", "postpublish",
        "preinstall", "install", "postinstall",
        "preuninstall", "uninstall", "postuninstall",
        "preversion", "version", "postversion",
    )

    /**
     * True when [name] is unconditionally treated as used: either one
     * of the fixed [ALWAYS_USED] lifecycle names, or a `pre`/`post`
     * prefix of another script that genuinely exists in
     * [allScriptNames] (npm runs `prebuild`/`postbuild` around a real
     * `build` script by convention -- but a lone `prefoo` with no
     * `foo` anywhere is not a lifecycle hook of anything, just an
     * oddly-named script that gets evaluated like any other).
     */
    fun isAlwaysUsed(name: String, allScriptNames: Set<String>): Boolean {
        if (name in ALWAYS_USED) return true

        val base = when {
            name.startsWith("pre") -> name.removePrefix("pre")
            name.startsWith("post") -> name.removePrefix("post")
            else -> return false
        }
        return base.isNotEmpty() && base in allScriptNames
    }
}
