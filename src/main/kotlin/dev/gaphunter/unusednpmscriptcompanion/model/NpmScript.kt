package dev.gaphunter.unusednpmscriptcompanion.model

/**
 * One entry of a `package.json`'s `"scripts"` object, as written --
 * name and raw command text, plus the exact text offset of the name's
 * string literal so a [com.intellij.psi.PsiElement] can be re-anchored
 * for a gutter icon. Extraction is dumb (this class), interpretation
 * (is it orphaned? is it a lifecycle hook?) happens in
 * [dev.gaphunter.unusednpmscriptcompanion.scan.UsageScanner] and
 * [dev.gaphunter.unusednpmscriptcompanion.scan.LifecycleScripts] --
 * same "extraction/interpretation split" already proven throughout
 * this catalog (e.g. `GradleBuildFileParser`).
 */
data class NpmScript(
    val name: String,
    val command: String,
    val nameLiteralStartOffset: Int,
)

/** Outcome of checking whether one [NpmScript] is used somewhere. */
enum class UsageVerdict {
    /** Called by another script in the same package.json (`npm run x && npm run y`). */
    USED_IN_OWN_PACKAGE_JSON,

    /** Referenced by `npm run <name>` / `yarn <name>` / `pnpm <name>` in a CI config file. */
    USED_IN_CI,

    /** Mentioned as `npm run <name>` / `yarn <name>` / `pnpm <name>` in README.md. */
    USED_IN_README,

    /** An npm lifecycle hook (or the pre/post of a real sibling script) -- never flagged. */
    LIFECYCLE_HOOK,

    /** No reference found anywhere this plugin looks -- a real orphan candidate. */
    ORPHANED,
}

/** One script plus the reason it is/isn't considered used. */
data class ScriptUsage(
    val script: NpmScript,
    val verdict: UsageVerdict,
    /** Human-readable detail for the gutter tooltip, e.g. "called from .github/workflows/ci.yml". */
    val detail: String,
)
