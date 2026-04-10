package com.liushoukun.infographic.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * 轻量词法分析，语义对齐仓库根目录 syntaxes/infographic.tmLanguage.json。
 */
class InfographicLexer : LexerBase() {

  private var buffer: CharSequence = ""
  private var bufferStart = 0
  private var bufferEnd = 0
  private var tokenStart = 0
  private var tokenEnd = 0
  private var tokenType: IElementType? = null
  /** 0 普通；1 双引号串；2 单引号串 */
  private var stringQuote = 0

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    this.buffer = buffer
    bufferStart = startOffset
    bufferEnd = endOffset
    tokenEnd = startOffset
    stringQuote = initialState and 0xFF
    advance()
  }

  override fun getState(): Int = stringQuote

  override fun getTokenType(): IElementType? = tokenType

  override fun getTokenStart(): Int = tokenStart

  override fun getTokenEnd(): Int = tokenEnd

  override fun getBufferSequence(): CharSequence = buffer

  override fun getBufferEnd(): Int = bufferEnd

  override fun advance() {
    tokenStart = tokenEnd
    if (tokenStart >= bufferEnd) {
      tokenType = null
      return
    }
    when (stringQuote) {
      1 -> {
        tokenType = scanDoubleQuotedString()
        return
      }
      2 -> {
        tokenType = scanSingleQuotedString()
        return
      }
    }

    val c = buffer[tokenEnd]
    when {
      c == '\r' || c == '\n' || c == '\t' || c == ' ' -> {
        tokenType = InfographicTokenTypes.WHITE_SPACE
        while (tokenEnd < bufferEnd) {
          val ch = buffer[tokenEnd]
          if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') break
          tokenEnd++
        }
      }
      c == '#' -> {
        tokenType = InfographicTokenTypes.LINE_COMMENT
        while (tokenEnd < bufferEnd && buffer[tokenEnd] != '\n') tokenEnd++
      }
      c == '"' -> {
        stringQuote = 1
        tokenEnd++
        tokenType = scanDoubleQuotedString()
      }
      c == '\'' -> {
        stringQuote = 2
        tokenEnd++
        tokenType = scanSingleQuotedString()
      }
      isLineStartBullet(tokenStart) && c == '-' -> {
        tokenType = InfographicTokenTypes.LIST_MARK
        tokenEnd++
        if (tokenEnd < bufferEnd && buffer[tokenEnd] == ' ') tokenEnd++
      }
      else -> scanWordOrSymbol()
    }
  }

  private fun isLineStartBullet(offset: Int): Boolean {
    var i = offset - 1
    while (i >= bufferStart) {
      val ch = buffer[i]
      if (ch == '\n') return true
      if (ch != ' ' && ch != '\t') return false
      i--
    }
    return true
  }

  private fun scanDoubleQuotedString(): IElementType {
    while (tokenEnd < bufferEnd) {
      val ch = buffer[tokenEnd]
      if (ch == '\\' && tokenEnd + 1 < bufferEnd) {
        tokenEnd += 2
        continue
      }
      if (ch == '"') {
        tokenEnd++
        stringQuote = 0
        return InfographicTokenTypes.STRING
      }
      tokenEnd++
    }
    stringQuote = 0
    return InfographicTokenTypes.STRING
  }

  private fun scanSingleQuotedString(): IElementType {
    while (tokenEnd < bufferEnd) {
      val ch = buffer[tokenEnd]
      if (ch == '\\' && tokenEnd + 1 < bufferEnd) {
        tokenEnd += 2
        continue
      }
      if (ch == '\'') {
        tokenEnd++
        stringQuote = 0
        return InfographicTokenTypes.STRING
      }
      tokenEnd++
    }
    stringQuote = 0
    return InfographicTokenTypes.STRING
  }

  private fun Char.isIdBody(): Boolean = isLetterOrDigit() || this == '-' || this == '_'

  private fun scanWordOrSymbol() {
    val start = tokenEnd
    val c = buffer[start]
    if (!c.isLetter() && c != '_' && c != '-') {
      tokenEnd++
      tokenType = InfographicTokenTypes.BAD_CHARACTER
      return
    }
    while (tokenEnd < bufferEnd && buffer[tokenEnd].isIdBody()) tokenEnd++
    val word = buffer.subSequence(start, tokenEnd).toString()
    val atLineKeyword = isAtLineStartAfterIndent(start)
    tokenType = classifyWord(word.lowercase(), atLineKeyword, start)
  }

  private fun isAtLineStartAfterIndent(offset: Int): Boolean {
    var i = offset - 1
    while (i >= bufferStart) {
      val ch = buffer[i]
      if (ch == '\n') return true
      if (ch != ' ' && ch != '\t') return false
      i--
    }
    return true
  }

  private fun classifyWord(w: String, atLineKeyword: Boolean, startOffset: Int): IElementType {
    if (atLineKeyword) {
      if (w == "infographic" || w in CONTROL_KEYWORDS) return InfographicTokenTypes.KEYWORD
      if (w in PROPERTY_NAMES) return InfographicTokenTypes.PROPERTY
      if (w.contains('-') && isAfterInfographicKeyword(startOffset)) {
        return InfographicTokenTypes.TYPE_REF
      }
    }
    return InfographicTokenTypes.IDENTIFIER
  }

  /** 当前词紧接在同一行「infographic」关键字与空白之后（用于模板类型 slug，如 list-row-simple）。 */
  private fun isAfterInfographicKeyword(wordStart: Int): Boolean {
    var lineStart = wordStart - 1
    while (lineStart >= bufferStart && buffer[lineStart] != '\n') lineStart--
    lineStart++
    val raw = buffer.subSequence(lineStart, wordStart).toString()
    return raw.matches(Regex("^\\s*infographic\\s+$", RegexOption.IGNORE_CASE))
  }

  companion object {
    private val CONTROL_KEYWORDS = setOf("data", "theme", "template", "design")
    private val PROPERTY_NAMES = setOf("label", "desc", "title", "value", "name", "type")
  }
}
