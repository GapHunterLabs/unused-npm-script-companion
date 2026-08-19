package dev.gaphunter.unusednpmscriptcompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import dev.gaphunter.unusednpmscriptcompanion.model.NpmScript
import dev.gaphunter.unusednpmscriptcompanion.model.ScriptUsage
import dev.gaphunter.unusednpmscriptcompanion.model.UsageVerdict
import dev.gaphunter.unusednpmscriptcompanion.parse.PackageJsonParser
import dev.gaphunter.unusednpmscriptcompanion.scan.CiFileLocator
import dev.gaphunter.unusednpmscriptcompanion.scan.UsageScanner
import java.nio.charset.StandardCharsets

/**
 * Gutter icon per `"scripts"` entry of an open `package.json`, showing
 * whether it's used or orphaned.
 *
 * **Why a gutter icon (`LineMarkerProviderDescriptor`), not an inlay
 * hint:** the three sibling plugins in this batch (Regex Named Group,
 * HTTP Status Inline, Dockerfile Layer Size Companion) all render an
 * inlay because their feature *adds information to a specific point on
 * the line* (a captured group's name, a status's official name, a
 * layer's byte size) right where the reader's eye already is. This
 * feature is different in kind: it's a per-line **verdict** (used vs.
 * orphaned) about the *whole script entry*, not a value attached to one
 * token -- exactly the shape `highlight-companion`'s
 * `CognitiveComplexityLineMarkerProvider` already ships successfully
 * for cognitive-complexity scores per method. A gutter icon also scans
 * better for the actual task ("which of these 15 scripts can I safely
 * delete?") than an inlay would: a column of icons is visually
 * scannable top-to-bottom in a way inline text after each line is not.
 *
 * **Off-EDT discipline:** all real work -- parsing the CI/README files
 * from disk, running [UsageScanner] -- happens in
 * `collectSlowLineMarkers`, which the platform contract guarantees runs
 * on a background thread as part of the daemon's slow-line-markers
 * pass, never on the EDT. `getLineMarkerInfo` (the fast per-element
 * pass) is a hard no-op, same split as `CognitiveComplexityLineMarkerProvider`.
 */
class UnusedNpmScriptLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "Unused npm scripts"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val jsonFile = elements.firstOrNull()?.containingFile as? JsonFile ?: return
        if (!isPackageJson(jsonFile)) return

        val scripts = PackageJsonParser.parseScripts(jsonFile)
        if (scripts.isEmpty()) return

        val usages = scanUsages(jsonFile, scripts)
        val usageByOffset = usages.associateBy { it.script.nameLiteralStartOffset }

        for (element in elements) {
            val nameLiteral = nameLiteralOf(element) ?: continue
            val usage = usageByOffset[nameLiteral.textRange.startOffset] ?: continue
            result.add(buildMarker(nameLiteral, usage))
        }
    }

    private fun isPackageJson(file: JsonFile): Boolean = file.virtualFile?.name == "package.json"

    /**
     * Matches the [JsonStringLiteral] that is the *name* (key) of a
     * `"scripts"` property -- the element the platform actually visits
     * during the slow-line-marker pass is the string literal itself,
     * one of potentially many string literals in the file, so this
     * must confirm both "is a property name" and "is under scripts".
     */
    private fun nameLiteralOf(element: PsiElement): JsonStringLiteral? {
        val literal = element as? JsonStringLiteral ?: return null
        val property = literal.parent as? JsonProperty ?: return null
        if (property.nameElement !== literal) return null
        return literal
    }

    private fun scanUsages(jsonFile: JsonFile, scripts: List<NpmScript>): List<ScriptUsage> {
        val packageJsonVFile = jsonFile.virtualFile
        val dir: VirtualFile? = packageJsonVFile?.parent

        val ciFiles: Map<String, String> = if (dir != null) {
            CiFileLocator.findCiFiles(dir).associate { file -> relativeCiLabel(dir, file) to readTextSafely(file) }
        } else {
            emptyMap()
        }

        val readmeText = dir?.let { CiFileLocator.findReadme(it) }?.let { readTextSafely(it) }

        return UsageScanner.scan(scripts, ciFiles, readmeText)
    }

    private fun relativeCiLabel(dir: VirtualFile, ciFile: VirtualFile): String {
        val dirPath = dir.path
        val filePath = ciFile.path
        return if (filePath.startsWith(dirPath)) filePath.removePrefix(dirPath).trimStart('/', '\\') else ciFile.name
    }

    private fun readTextSafely(file: VirtualFile): String = try {
        String(file.contentsToByteArray(), StandardCharsets.UTF_8)
    } catch (_: Exception) {
        ""
    }

    /**
     * [LineMarkerInfo] must be anchored on a **leaf** PSI element -- the
     * platform logs a "Performance warning" (which fails tests, and in
     * a real IDE session spams the log) if given a composite element
     * like [JsonStringLiteral] itself. The literal's `firstChild` is the
     * actual leaf token (`DOUBLE_QUOTED_STRING`); [nameLiteral]'s own
     * [com.intellij.psi.PsiElement.getTextRange] is still used for the
     * marker's visual range, so the icon still spans the full `"name"`
     * token, not just its first character.
     */
    private fun buildMarker(nameLiteral: JsonStringLiteral, usage: ScriptUsage): LineMarkerInfo<PsiElement> {
        val leaf = nameLiteral.firstChild ?: nameLiteral
        val tooltip = tooltipFor(usage)
        return LineMarkerInfo(
            leaf,
            nameLiteral.textRange,
            iconFor(usage.verdict),
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }

    private fun iconFor(verdict: UsageVerdict) = when (verdict) {
        UsageVerdict.ORPHANED -> UnusedNpmScriptIcons.ORPHANED
        else -> UnusedNpmScriptIcons.USED
    }

    private fun tooltipFor(usage: ScriptUsage): String {
        val prefix = when (usage.verdict) {
            UsageVerdict.ORPHANED -> "Possibly unused npm script"
            else -> "Used npm script"
        }
        return "$prefix \"${usage.script.name}\": ${usage.detail}"
    }
}
