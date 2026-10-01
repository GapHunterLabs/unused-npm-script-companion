package dev.gaphunter.unusednpmscriptcompanion.scan

/**
 * Finds real invocations of an npm script by name inside arbitrary
 * text (another script's own command, a CI YAML file, README.md prose)
 * -- deliberately narrow, so a script named e.g. `"build"` is never
 * considered "referenced" just because the word "build" appears
 * somewhere unrelated in a README paragraph. Only the real runner
 * invocation shapes count:
 *
 * - `npm run <name>` and its alias `npm run-script <name>`
 * - `yarn <name>` and `yarn run <name>`
 * - `pnpm <name>` and `pnpm run <name>`
 * - `bun <name>` and `bun run <name>`
 * - any of the above with `-x` / `--flag` / `--flag=value` options
 *   between the runner and the name (`npm run -s build`,
 *   `npm run --if-present lint`)
 * - `run-s`, `run-p` and `npm-run-all` (the npm-run-all CLIs), whose
 *   arguments are script names or globs (`run-s clean compile`,
 *   `npm-run-all --parallel lint:*`)
 *
 * Before 0.2.3 only `npm run`, `yarn <name>` and `pnpm [run]` were
 * recognized: a script called with `yarn run e2e` (a common CI form), or
 * from another script through `run-s`/`run-p`, was reported as "possibly
 * unused" while it was in use (found 2026-10-01).
 *
 * `npm start`/`npm test`/`npm stop`/`npm restart` (no `run`) are
 * intentionally NOT matched here -- those names are already
 * unconditionally treated as used via [LifecycleScripts].
 */
object ScriptReferenceMatcher {

    /**
     * A real npm script name can itself contain characters (`-`, `_`,
     * `:`, `.`) that are not "word characters" to a plain regex `\b` --
     * so a lone trailing `\b` after a script name is not enough to
     * reject `build-prod` when searching for `build` (`\b` fires at the
     * `d`|`-` boundary too, since `-` is non-word). This negative
     * lookahead is the real boundary: it rejects a match only when the
     * very next character would extend the name into a longer,
     * different script identifier.
     */
    private const val NAME_CONTINUATION = "[A-Za-z0-9_:.@/-]"

    /** Options around `run` and before the name: `-s`, `--silent`, `--if-present`, `--workspace=web`. */
    private const val OPTIONS = """(?:\s+--?[A-Za-z][\w-]*(?:=\S+)?)*"""

    private const val RUNNER = """\b(?:npm$OPTIONS\s+(?:run|run-script)|(?:yarn|pnpm|bun)(?:$OPTIONS\s+run)?)"""

    /** An npm-run-all CLI and its arguments, up to the end of that shell command. */
    private val RUN_ALL = Regex("""(?<![\w-])(?:npm-run-all|run-s|run-p)((?:[ \t]+[^\s;&|"'`]+)+)""")

    /**
     * True when [text] contains a real invocation of [scriptName] via
     * one of the runner shapes above. The script name is escaped and
     * bounded on the left by the runner keyword (or an option) before it
     * and on the right by [NAME_CONTINUATION]'s negative lookahead, so
     * `"build"` never matches inside `"build-prod"` or `"rebuild"`. A
     * Regex object is built fresh per call -- project sizes don't warrant
     * a caching layer here.
     */
    fun isReferenced(text: String, scriptName: String): Boolean {
        val escaped = Regex.escape(scriptName)
        if (Regex("""$RUNNER$OPTIONS\s+$escaped(?!$NAME_CONTINUATION)""").containsMatchIn(text)) return true
        return RUN_ALL.findAll(text).any { match ->
            match.groupValues[1].trim().split(Regex("""[ \t]+""")).any { arg -> !arg.startsWith("-") && argMatches(arg, scriptName) }
        }
    }

    /** An npm-run-all argument names a script exactly or with `*` / `**` globs (`lint:*`, `build:**`). */
    private fun argMatches(arg: String, scriptName: String): Boolean {
        if (!arg.contains('*')) return arg == scriptName
        val pattern = arg.split("**").joinToString(".*") { part ->
            part.split("*").joinToString("[^:]*") { Regex.escape(it) }
        }
        return Regex(pattern).matches(scriptName)
    }
}
