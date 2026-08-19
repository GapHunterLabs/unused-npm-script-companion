package dev.gaphunter.unusednpmscriptcompanion.gutter

import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * The two fixed gutter icons this plugin ever shows -- unlike
 * `highlight-companion`'s `ComplexityGutterIcon` (an arbitrary number
 * that must be drawn on demand), this feature only ever has two
 * states, so reusing the platform's own bundled icons is both simpler
 * and more consistent with the rest of the IDE's chrome than a custom
 * `Icon` implementation. Confirmed to exist on the pinned platform
 * version (2025.2.6.2) via `javap` against `AllIcons$General`, per
 * `CONSTITUTION.md` §6 -- same "verify the real member, don't guess"
 * discipline already used for `PasswordSafe`/`CredentialStore` in
 * `gitlab-ci-companion`.
 */
object UnusedNpmScriptIcons {
    /** Referenced somewhere (own package.json, CI config, README) or an npm lifecycle hook. */
    val USED: Icon = AllIcons.General.InspectionsOKEmpty

    /** No reference found anywhere this plugin looks -- a real orphan candidate. */
    val ORPHANED: Icon = AllIcons.General.InspectionsWarningEmpty
}
