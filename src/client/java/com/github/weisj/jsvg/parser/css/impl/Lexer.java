package com.github.weisj.jsvg.parser.css.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import java.text.MessageFormat;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public final class Lexer {
   private static final Logger LOGGER = LogFactory.createLogger(Lexer.class);
   @NotNull
   private final List<char[]> input;
   private int listIndex = 0;
   private int index = 0;
   private boolean inRuleDefinition;
   private boolean parsingRaw;
   private char current;

   public Lexer(@NotNull List<char[]> input, SimpleCssParser.Mode mode) {
      this.input = input;
      this.inRuleDefinition = mode == SimpleCssParser.Mode.SINGLE_RULE;
      this.current = 0;
   }

   @NotNull
   public Token nextToken() {
      this.consumeWhiteSpace();
      if (this.inRuleDefinition && this.parsingRaw) {
         this.parsingRaw = false;
         return new Token(TokenType.RAW_DATA, this.readWhile(cx -> cx != ';' && cx != '}'));
      }

      if (this.isEof()) {
         return new Token(TokenType.EOF);
      }

      char c = this.current();
      switch (c) {
         case '#':
            this.next();
            return new Token(TokenType.ID_NAME, this.readIdentifier());
         case ',':
            this.next();
            return new Token(TokenType.COMMA);
         case '.':
            this.next();
            return new Token(TokenType.CLASS_NAME, this.readIdentifier());
         case '/':
            if (this.peekNext() == '*') {
               this.next();
               this.next();
               String comment = this.readWhile(n -> n != '*' || this.peekNext() != '/');
               this.next();
               this.next();
               return new Token(TokenType.COMMENT, comment);
            }
         default:
            return new Token(TokenType.IDENTIFIER, this.readIdentifier());
         case ':':
            this.parsingRaw = true;
            this.next();
            return new Token(TokenType.COLON);
         case ';':
            this.next();
            return new Token(TokenType.SEMICOLON);
         case '{':
            this.inRuleDefinition = true;
            this.parsingRaw = false;
            this.next();
            return new Token(TokenType.CURLY_OPEN);
         case '}':
            this.inRuleDefinition = false;
            this.parsingRaw = false;
            this.next();
            return new Token(TokenType.CURLY_CLOSE);
      }
   }

   private boolean isEof() {
      return this.listIndex >= this.input.size() || this.listIndex == this.input.size() - 1 && this.index >= this.input.get(this.listIndex).length;
   }

   private void consumeWhiteSpace() {
      while (Character.isWhitespace(this.current())) {
         this.next();
      }
   }

   private boolean isIdentifierCharStart(char c) {
      if ('A' <= c && c <= 'Z') {
         return true;
      } else if ('a' <= c && c <= 'z') {
         return true;
      } else {
         return c == '-' ? true : c == '_';
      }
   }

   private boolean isIdentifierChar(char c) {
      return this.isIdentifierCharStart(c) ? true : '0' <= c && c <= '9';
   }

   @NotNull
   private String readIdentifier() {
      if (this.isIdentifierCharStart(this.current()) && this.isIdentifierChar(this.current())) {
         return this.readWhile(this::isIdentifierChar);
      }

      LOGGER.log(Logger.Level.WARNING, () -> MessageFormat.format("Identifier starting with unexpected char ''{0}''", this.current()));
      if (this.readWhile(this::isIdentifierChar).isEmpty()) {
         this.next();
      }

      throw new ParserException();
   }

   @NotNull
   private String readWhile(@NotNull Predicate<Character> filter) {
      if (this.isEof()) {
         return "";
      }

      int startListIndex = this.listIndex;
      int startIndex = this.index;

      while (!this.isEof() && filter.test(this.current())) {
         this.next();
      }

      int endListIndex = this.isEof() ? this.input.size() - 1 : this.listIndex;
      int endIndex = this.isEof() ? this.input.get(endListIndex).length : this.index;
      StringBuilder builder = new StringBuilder();
      int start = startIndex;

      for (int i = startListIndex; i <= endListIndex; i++) {
         char[] segment = this.input.get(i);
         int end = i == endListIndex ? endIndex : segment.length;
         builder.append(String.valueOf(segment, start, end - start));
         start = 0;
      }

      return builder.toString();
   }

   private char current() {
      if (this.current == 0) {
         this.current = this.nextChar();
      }

      return this.current;
   }

   private char nextChar() {
      return this.isEof() ? '\u0000' : this.input.get(this.listIndex)[this.index];
   }

   private char peekNext() {
      if (this.isEof()) {
         return '\u0000';
      }

      if (this.index + 1 < this.input.get(this.listIndex).length) {
         return this.input.get(this.listIndex)[this.index + 1];
      }

      for (int currentIndex = this.listIndex + 1; currentIndex < this.input.size(); currentIndex++) {
         if (this.input.get(currentIndex).length > 0) {
            return this.input.get(currentIndex)[0];
         }
      }

      return '\u0000';
   }

   private void next() {
      this.index++;
      if (this.index >= this.input.get(this.listIndex).length && this.listIndex + 1 < this.input.size()) {
         this.index = 0;
         this.listIndex++;
      }

      this.current = this.nextChar();
   }
}

