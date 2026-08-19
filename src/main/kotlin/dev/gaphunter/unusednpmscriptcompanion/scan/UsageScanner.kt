package dev.gaphunter.unusednpmscriptcompanion.scan

import dev.gaphunter.unusednpmscriptcompanion.model.NpmScript
import dev.gaphunter.unusednpmscriptcompanion.model.ScriptUsage
import dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict

/**
 * Decides, for each [NpmScript] in a `package.json`, whether it's used
 * and why -- pure text/data in, [ScriptUsage] list out, no PSI/VFS
 * dependency at all. This is deliberately the layer that's cheap to
 * unit-test exhaustively; [dev.gaphunter.unusednpmscriptcompanion.gutter.UnusedNpmScriptLineMarkerProvider]
 * is the thin platform-facing layer on top that gathers the raw text
 * inputs ([CiFileLocator] + [VirtualFile.readText]) and calls this.
 *
 * Verdict priority, checked in this fixed order per script:
 * 1. [dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict.LIFECYCLE_HOOK] --
 *    an npm-lifecycle name or a real pre/post of a sibling script,
 *    checked first because it's a structural fact about npm itself,
 *    unrelated to whether any text reference exists.
 * 2. [dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict.USED_IN_OWN_PACKAGE_JSON] --
 *    another script's own command text calls it.
 * 3. [dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict.USED_IN_CI] --
 *    a CI config file calls it.
 * 4. [dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict.USED_IN_README] --
 *    README.md mentions calling it.
 * 5. [dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict.ORPHANED] --
 *    none of the above found any reference.
 */
object UsageScanner {

    /**
     * @param scripts the parsed `"scripts"` entries of one package.json.
     * @param ciFiles CI config file name -> full text content (v0.1: GitHub Actions + GitLab CI, see [CiFileLocator]).
     * @param readmeText README.md content, or null if the project has none.
     */
    fun scan(
        scripts: List<NpmScript>,
        ciFiles: Map<String, String>,
        readmeText: String?,
    ): List<ScriptUsage> {
        val allNames = scripts.map { it.name }.toSet()

        return scripts.map { script -> evaluate(script, scripts, allNames, ciFiles, readmeText) }
    }

    private fun evaluate(
        script: NpmScript,
        allScripts: List<NpmScript>,
        allNames: Set<String>,
        ciFiles: Map<String, String>,
        readmeText: String?,
    ): ScriptUsage {
        if (LifecycleScripts.isAlwaysUsed(script.name, allNames)) {
            return ScriptUsage(script, UsageVerdict.LIFECYCLE_HOOK, "npm lifecycle convention -- always runs automatically")
        }

        val caller = allScripts.firstOrNull { other ->
            other.name != script.name && ScriptReferenceMatcher.isReferenced(other.command, script.name)
        }
        if (caller != null) {
            return ScriptUsage(
                script,
                UsageVerdict.USED_IN_OWN_PACKAGE_JSON,
                "called from this package.json's own \"${caller.name}\" script",
            )
        }

        val ciMatch = ciFiles.entries.firstOrNull { (_, text) -> ScriptReferenceMatcher.isReferenced(text, script.name) }
        if (ciMatch != null) {
            return ScriptUsage(script, UsageVerdict.USED_IN_CI, "called from ${ciMatch.key}")
        }

        if (readmeText != null && ScriptReferenceMatcher.isReferenced(readmeText, script.name)) {
            return ScriptUsage(script, UsageVerdict.USED_IN_README, "mentioned in README.md")
        }

        return ScriptUsage(script, UsageVerdict.ORPHANED, "no reference found in this package.json, GitHub Actions/GitLab CI configs, or README.md")
    }
}
