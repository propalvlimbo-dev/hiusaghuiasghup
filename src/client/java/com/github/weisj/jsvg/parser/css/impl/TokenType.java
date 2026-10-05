package com.github.weisj.jsvg.parser.css.impl;

public enum TokenType {
   START,
   CURLY_OPEN,
   CURLY_CLOSE,
   COMMENT,
   COLON,
   COMMA,
   SEMICOLON,
   RAW_DATA,
   CLASS_NAME,
   ID_NAME,
   IDENTIFIER,
   EOF;

   // $VF: synthetic method
   private static TokenType[] $values() {
      return new TokenType[]{START, CURLY_OPEN, CURLY_CLOSE, COMMENT, COLON, COMMA, SEMICOLON, RAW_DATA, CLASS_NAME, ID_NAME, IDENTIFIER, EOF};
   }
}
