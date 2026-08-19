package dev.gaphunter.unusednpmscriptcompanion.scan

/**
 * Finds real invocations of an npm script by name inside arbitrary
 * text (another script's own command, a CI YAML file, README.md prose)
 * -- deliberately narrow, so a script named e.g. `"build"` is never
 * considered "referenced" just because the word "build" appears
 * somewhere unrelated in a README paragraph. Only the real runner
 * invocation shapes count:
 *
 * - `npm run <name>` (the canonical form for any script)
 * - `yarn <name>` (Yarn's shorthand -- no `run` keyword needed)
 * - `pnpm run <name>` and `pnpm <name>` (pnpm accepts both forms)
 *
 * `npm start`/`npm test` (no `run`) are intentionally NOT matched here
 * -- those two names are already unconditionally treated as used via
 * [LifecycleScripts], so under-matching them here costs nothing and
 * keeps this matcher's contract simple: every pattern it recognizes
 * requires an explicit runner keyword immediately before the name.
 */
object ScriptReferenceMatcher {

    /**
     * A real npm script name can itself contain characters (`-`, `_`,
     * `:`, `.`) that are not "word characters" to a plain regex `\b` --
     * so a lone trailing `\b` after [scriptName] is not enough to
     * reject `build-prod` when searching for `build` (`\b` fires at the
     * `d`|`-` boundary too, since `-` is non-word). This negative
     * lookahead is the real boundary: it rejects a match only when the
     * very next character would extend the name into a longer,
     * different script identifier.
     */
    private val NAME_CONTINUATION = "[A-Za-z0-9_:.@/-]"

    /**
     * True when [text] contains a real invocation of [scriptName] via
     * one of the runner shapes above. The script name is escaped and
     * bounded on the left by `\b`/whitespace (guaranteed by the runner
     * keyword before it) and on the right by [NAME_CONTINUATION]'s
     * negative lookahead, so `"build"` never matches inside
     * `"build-prod"` or `"rebuild"`. A Regex object is built fresh per
     * call -- v0.1's project sizes don't warrant a caching layer here.
     */
    fun isReferenced(text: String, scriptName: String): Boolean {
        val escaped = Regex.escape(scriptName)
        val pattern = Regex(
            """\b(?:npm\s+run|yarn|pnpm(?:\s+run)?)\s+$escaped(?!$NAME_CONTINUATION)""",
        )
        return pattern.containsMatchIn(text)
    }
}
