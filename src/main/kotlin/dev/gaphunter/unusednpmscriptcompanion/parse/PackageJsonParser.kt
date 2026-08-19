package dev.gaphunter.unusednpmscriptcompanion.parse

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import dev.gaphunter.unusednpmscriptcompanion.model.NpmScript

/**
 * Reads the `"scripts"` object of an already-parsed `package.json`
 * ([JsonFile]) via the bundled JSON plugin's real PSI -- no hand-rolled
 * JSON lexer, same "don't reinvent a parser for a format the platform
 * already parses correctly" principle already proven in
 * `json-schema-companion`/`json-to-code-companion`. A malformed JSON
 * file simply produces PSI that doesn't resolve to the expected shape,
 * which this returns as an honest empty list -- never a crash.
 */
object PackageJsonParser {

    /**
     * Returns every entry of `"scripts"`, in source order. Returns an
     * empty list -- never throws -- when the file has no top-level
     * object, no `"scripts"` property, or `"scripts"` isn't itself an
     * object (all real, honest states a hand-edited package.json can be
     * in mid-edit).
     */
    fun parseScripts(file: JsonFile): List<NpmScript> {
        val root = file.topLevelValue as? JsonObject ?: return emptyList()
        val scriptsProperty = root.findProperty("scripts") ?: return emptyList()
        val scriptsObject = scriptsProperty.value as? JsonObject ?: return emptyList()

        return scriptsObject.propertyList.mapNotNull { property -> toNpmScript(property) }
    }

    /** True when the file has a `"scripts"` object at all (even if empty). */
    fun hasScriptsSection(file: JsonFile): Boolean {
        val root = file.topLevelValue as? JsonObject ?: return false
        val scriptsProperty = root.findProperty("scripts") ?: return false
        return scriptsProperty.value is JsonObject
    }

    private fun toNpmScript(property: JsonProperty): NpmScript? {
        val nameLiteral = property.nameElement as? JsonStringLiteral ?: return null
        val valueLiteral = property.value as? JsonStringLiteral ?: return null
        return NpmScript(
            name = property.name,
            command = valueLiteral.value,
            nameLiteralStartOffset = nameLiteral.textRange.startOffset,
        )
    }
}
