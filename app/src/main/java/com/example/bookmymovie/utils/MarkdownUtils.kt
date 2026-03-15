package com.example.bookmymovie.utils

object MarkdownUtils {

    /**
     * Strips common markdown syntax so the text renders as clean, readable plain text.
     * Converts bullet markers to bullet points, removes bold/italic/code markers, etc.
     */
    fun stripMarkdown(text: String): String {
        var result = text

        // Code blocks (``` ... ```) — remove fences, keep content
        result = result.replace(Regex("```[a-zA-Z]*\\n?"), "")
        result = result.replace("```", "")

        // Inline code `text` → text
        result = result.replace(Regex("`([^`]+)`"), "$1")

        // Bold **text** or __text__
        result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        result = result.replace(Regex("__(.+?)__"), "$1")

        // Italic *text* or _text_ (only single asterisk/underscore)
        result = result.replace(Regex("(?<![*])\\*(?![*\\s])(.+?)(?<![\\s*])\\*(?![*])"), "$1")
        result = result.replace(Regex("(?<!_)_(?![_\\s])(.+?)(?<![\\s_])_(?!_)"), "$1")

        // Strikethrough ~~text~~
        result = result.replace(Regex("~~(.+?)~~"), "$1")

        // Headings # ## ### at start of line
        result = result.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")

        // Horizontal rules --- or *** or ___
        result = result.replace(Regex("^\\s*([-*_]){3,}\\s*$", RegexOption.MULTILINE), "")

        // Blockquotes > at start of line
        result = result.replace(Regex("^>\\s*", RegexOption.MULTILINE), "")

        // Unordered list markers - or * or + at start of line → bullet point
        result = result.replace(Regex("^\\s*[-*+]\\s+", RegexOption.MULTILINE), "• ")

        // Links [text](url) → text
        result = result.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")

        // Replace → arrow with a cleaner › character
        result = result.replace("→", "›")

        // Collapse multiple blank lines to single blank line
        result = result.replace(Regex("\\n{3,}"), "\n\n")

        return result.trim()
    }

    /**
     * Strips text aggressively for Text-to-Speech so no symbols are read aloud.
     */
    fun stripForTTS(text: String): String {
        var result = stripMarkdown(text)
        // Replace visual symbols that TTS would read as words
        result = result.replace("›", ", ")
        result = result.replace("→", ", ")
        result = result.replace("—", ", ")
        result = result.replace("–", ", ")
        result = result.replace("•", "")
        result = result.replace("|", "")
        result = result.replace(Regex("[*_`#~>]"), "")
        // Collapse multiple spaces/punctuation
        result = result.replace(Regex("[ \\t]{2,}"), " ")
        result = result.replace(Regex("(, ){2,}"), ", ")
        return result.trim()
    }
}
