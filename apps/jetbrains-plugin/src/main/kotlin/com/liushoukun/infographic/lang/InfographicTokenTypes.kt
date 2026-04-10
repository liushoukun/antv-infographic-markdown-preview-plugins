package com.liushoukun.infographic.lang

import com.intellij.psi.tree.IElementType

object InfographicTokenTypes {
  @JvmField
  val WHITE_SPACE: IElementType = IElementType("INF_WHITE_SPACE", InfographicLanguage)

  @JvmField
  val LINE_COMMENT: IElementType = IElementType("INF_LINE_COMMENT", InfographicLanguage)

  @JvmField
  val STRING: IElementType = IElementType("INF_STRING", InfographicLanguage)

  @JvmField
  val KEYWORD: IElementType = IElementType("INF_KEYWORD", InfographicLanguage)

  @JvmField
  val PROPERTY: IElementType = IElementType("INF_PROPERTY", InfographicLanguage)

  @JvmField
  val LIST_MARK: IElementType = IElementType("INF_LIST_MARK", InfographicLanguage)

  @JvmField
  val TYPE_REF: IElementType = IElementType("INF_TYPE_REF", InfographicLanguage)

  @JvmField
  val IDENTIFIER: IElementType = IElementType("INF_IDENTIFIER", InfographicLanguage)

  @JvmField
  val BAD_CHARACTER: IElementType = IElementType("INF_BAD_CHARACTER", InfographicLanguage)
}
