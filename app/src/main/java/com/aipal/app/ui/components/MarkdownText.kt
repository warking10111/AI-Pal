package com.aipal.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val blocks = parseMarkdownBlocks(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockLayout(context, block.code, block.language)
                }
                is MarkdownBlock.Heading -> {
                    Text(
                        text = block.text,
                        fontSize = when (block.level) {
                            1 -> 24.sp
                            2 -> 20.sp
                            else -> 18.sp
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                        SelectionContainer {
                            Text(
                                text = buildFormattedText(block.text, textColor),
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    SelectionContainer {
                        Text(
                            text = buildFormattedText(block.text, textColor),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val text: String, val level: Int) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock()
}

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var inCodeBlock = false
    val currentCode = StringBuilder()
    var currentLanguage = ""

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                blocks.add(MarkdownBlock.CodeBlock(currentCode.toString().trimEnd(), currentLanguage))
                currentCode.clear()
                currentLanguage = ""
                inCodeBlock = false
            } else {
                // Start code block
                inCodeBlock = true
                currentLanguage = line.trim().substring(3).trim()
                if (currentLanguage.isEmpty()) currentLanguage = "code"
            }
            continue
        }

        if (inCodeBlock) {
            currentCode.append(line).append("\n")
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val title = trimmed.substring(level).trim()
            blocks.add(MarkdownBlock.Heading(title, level))
        } else if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
            val itemText = trimmed.substring(1).trim()
            blocks.add(MarkdownBlock.BulletItem(itemText))
        } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
            // Numbered list items are treated as bullet items for easy UI scanning
            val itemText = trimmed.replace(Regex("^\\d+\\.\\s"), "")
            blocks.add(MarkdownBlock.BulletItem(itemText))
        } else {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.CodeBlock(currentCode.toString(), currentLanguage))
    }

    return blocks
}

@Composable
private fun CodeBlockLayout(context: Context, code: String, language: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Column {
            // Header bar of code block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Code", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp).testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Code Text View
            SelectionContainer {
                Text(
                    text = buildHighlightedCode(code, language, MaterialTheme.colorScheme.onSurface),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(12.dp)
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun buildHighlightedCode(code: String, language: String, defaultColor: Color) = buildAnnotatedString {
    val keywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "import", "package", "return", 
        "if", "else", "while", "for", "in", "try", "catch", "finally", "throw", "const", 
        "let", "function", "def", "from", "as", "true", "false", "null", "nil", "public", 
        "private", "protected", "internal", "override", "suspend", "this", "super", "break", 
        "continue", "when", "switch", "case", "default", "type", "struct", "fn", "impl", "mut"
    )

    val lines = code.lines()
    for (lineIndex in lines.indices) {
        val line = lines[lineIndex]
        var i = 0
        while (i < line.length) {
            if (line.substring(i).startsWith("//") || line.substring(i).startsWith("#")) {
                withStyle(style = SpanStyle(color = Color(0xFF64748B), fontStyle = FontStyle.Italic)) {
                    append(line.substring(i))
                }
                break
            }
            
            if (line[i] == '"' || line[i] == '\'') {
                val quote = line[i]
                val start = i
                i++
                while (i < line.length && line[i] != quote) {
                    if (line[i] == '\\' && i + 1 < line.length) {
                        i += 2
                    } else {
                        i++
                    }
                }
                val end = (i + 1).coerceAtMost(line.length)
                withStyle(style = SpanStyle(color = Color(0xFF10B981))) {
                    append(line.substring(start, end))
                }
                i = end
                continue
            }
            
            if (line[i].isLetterOrDigit() || line[i] == '_') {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                    i++
                }
                val word = line.substring(start, i)
                if (keywords.contains(word)) {
                    withStyle(style = SpanStyle(color = Color(0xFFD946EF), fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                } else if (word.all { it.isDigit() }) {
                    withStyle(style = SpanStyle(color = Color(0xFFF59E0B))) {
                        append(word)
                    }
                } else {
                    withStyle(style = SpanStyle(color = defaultColor)) {
                        append(word)
                    }
                }
                continue
            }
            
            withStyle(style = SpanStyle(color = defaultColor.copy(alpha = 0.85f))) {
                append(line[i].toString())
            }
            i++
        }
        if (lineIndex < lines.size - 1) {
            append("\n")
        }
    }
}

@Composable
private fun buildFormattedText(text: String, defaultColor: Color) = buildAnnotatedString {
    // Basic regex support for inline bolding `**text**` and code `code`
    var currentIndex = 0
    val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
    val inlineCodePattern = Regex("`(.*?)`")

    val matches = (boldPattern.findAll(text) + inlineCodePattern.findAll(text))
        .sortedBy { it.range.first }

    for (match in matches) {
        if (match.range.first > currentIndex) {
            withStyle(style = SpanStyle(color = defaultColor)) {
                append(text.substring(currentIndex, match.range.first))
            }
        }

        val matchValue = match.groupValues[1]
        if (match.value.startsWith("**")) {
            // Bold element
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                append(matchValue)
            }
        } else {
            // Inline Code
            withStyle(
                style = SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.surfaceVariant,
                    fontSize = 14.sp
                )
            ) {
                append(matchValue)
            }
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        withStyle(style = SpanStyle(color = defaultColor)) {
            append(text.substring(currentIndex))
        }
    }
}
