package com.brahm.crm.util

import android.content.Context
import android.net.Uri

/**
 * A recipient row from an uploaded sheet: a phone, a display name, and any
 * extra columns (id, custom fields) usable as {placeholders} in a message.
 */
data class SheetRow(
    val phone: String,
    val name: String,
    val fields: Map<String, String>
)

object SheetImport {

    /**
     * Parses a CSV the user uploaded. The first row is treated as headers.
     * A column named (case-insensitive) phone/number/mobile → phone;
     * name → name. All columns (incl. id and custom ones) become {placeholders}.
     */
    fun parseCsv(context: Context, uri: Uri): List<SheetRow> {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return emptyList()
        val lines = text.split("\n", "\r\n", "\r").map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headers = splitCsvLine(lines.first()).map { it.trim() }
        val phoneIdx = headers.indexOfFirst { it.equals("phone", true) || it.equals("number", true) || it.equals("mobile", true) }
        val nameIdx = headers.indexOfFirst { it.equals("name", true) }

        val rows = ArrayList<SheetRow>()
        for (i in 1 until lines.size) {
            val cols = splitCsvLine(lines[i])
            if (cols.isEmpty()) continue
            val fields = LinkedHashMap<String, String>()
            headers.forEachIndexed { idx, h ->
                fields[h] = cols.getOrNull(idx)?.trim().orEmpty()
            }
            val phone = (if (phoneIdx >= 0) cols.getOrNull(phoneIdx) else cols.firstOrNull())
                ?.filter { it.isDigit() || it == '+' }.orEmpty()
            val name = (if (nameIdx >= 0) cols.getOrNull(nameIdx) else null)?.trim().orEmpty()
            if (phone.isNotEmpty()) rows.add(SheetRow(phone, name.ifEmpty { phone }, fields))
        }
        return rows
    }

    /** Replaces {column} tokens in the template with this row's values. */
    fun personalize(template: String, row: SheetRow): String {
        var out = template
        row.fields.forEach { (k, v) ->
            out = out.replace("{$k}", v, ignoreCase = true)
        }
        // Common aliases
        out = out.replace("{name}", row.name, ignoreCase = true)
            .replace("{phone}", row.phone, ignoreCase = true)
            .replace("{number}", row.phone, ignoreCase = true)
        return out
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                    else inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> { result.add(sb.toString()); sb.setLength(0) }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
