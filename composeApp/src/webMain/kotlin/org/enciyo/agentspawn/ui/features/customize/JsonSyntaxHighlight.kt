package org.enciyo.agentspawn.ui.features.customize

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

data class JsonHighlightColors(
    val key: Color = Color(0xFF9CDCFE),
    val string: Color = Color(0xFFCE9178),
    val number: Color = Color(0xFFB5CEA8),
    val keyword: Color = Color(0xFF569CD6),
    val punctuation: Color = Color(0xFFD4D4D4),
    val default: Color = Color(0xFFD4D4D4),
)

fun highlightJson(text: String, colors: JsonHighlightColors = JsonHighlightColors()): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString(text, colors)
}

private fun buildAnnotatedString(text: String, colors: JsonHighlightColors): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < text.length) {
        when (val c = text[i]) {
            '"' -> {
                val end = readJsonString(text, i)
                val content = text.substring(i, end)
                val isKey = isJsonKey(text, end)
                builder.withStyle(
                    SpanStyle(
                        color = if (isKey) colors.key else colors.string,
                        fontWeight = if (isKey) FontWeight.Medium else FontWeight.Normal,
                    )
                ) {
                    append(content)
                }
                i = end
            }
            in '0'..'9', '-' -> {
                if (c == '-' && (i + 1 >= text.length || !text[i + 1].isDigit())) {
                    builder.withStyle(SpanStyle(color = colors.punctuation)) { append(c) }
                    i++
                } else {
                    val end = readNumber(text, i)
                    builder.withStyle(SpanStyle(color = colors.number)) {
                        append(text.substring(i, end))
                    }
                    i = end
                }
            }
            else -> when {
                text.startsWith("true", i) && isKeywordBoundary(text, i, 4) -> {
                    builder.withStyle(SpanStyle(color = colors.keyword)) { append("true") }
                    i += 4
                }
                text.startsWith("false", i) && isKeywordBoundary(text, i, 5) -> {
                    builder.withStyle(SpanStyle(color = colors.keyword)) { append("false") }
                    i += 5
                }
                text.startsWith("null", i) && isKeywordBoundary(text, i, 4) -> {
                    builder.withStyle(SpanStyle(color = colors.keyword)) { append("null") }
                    i += 4
                }
                c in "{}[],:" -> {
                    builder.withStyle(SpanStyle(color = colors.punctuation)) { append(c) }
                    i++
                }
                else -> {
                    builder.withStyle(SpanStyle(color = colors.default)) { append(c) }
                    i++
                }
            }
        }
    }
    return builder.toAnnotatedString()
}

/** Reads a JSON string starting at [start] (must be '"'). Returns index after closing quote. */
private fun readJsonString(text: String, start: Int): Int {
    var i = start + 1
    while (i < text.length) {
        when (text[i]) {
            '\\' -> i += 2
            '"' -> return i + 1
            else -> i++
        }
    }
    return text.length
}

private fun isJsonKey(text: String, afterString: Int): Boolean {
    var i = afterString
    while (i < text.length && text[i].isWhitespace()) i++
    return i < text.length && text[i] == ':'
}

private fun readNumber(text: String, start: Int): Int {
    var i = start
    if (text[i] == '-') i++
    while (i < text.length && (text[i].isDigit() || text[i] == '.' || text[i] == 'e' || text[i] == 'E' || text[i] == '+')) {
        i++
    }
    return i
}

private fun isKeywordBoundary(text: String, start: Int, length: Int): Boolean {
    val end = start + length
    if (end > text.length) return false
    val beforeOk = start == 0 || !text[start - 1].isLetterOrDigit()
    val afterOk = end == text.length || !text[end].isLetterOrDigit()
    return beforeOk && afterOk
}

class JsonSyntaxHighlightVisualTransformation(
    private val colors: JsonHighlightColors = JsonHighlightColors(),
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlightJson(text.text, colors), OffsetMapping.Identity)
}

@Composable
fun rememberJsonSyntaxHighlightTransformation(
    colors: JsonHighlightColors = JsonHighlightColors(),
): VisualTransformation = remember(colors) {
    JsonSyntaxHighlightVisualTransformation(colors)
}
