package com.github.weisj.jsvg.parser.css.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.css.CssParser;
import com.github.weisj.jsvg.parser.css.StyleProperty;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class SimpleCssParser implements CssParser {
   private static final Logger LOGGER = LogFactory.createLogger(SimpleCssParser.class);

   @NotNull
   public SimpleStyleSheet parse(@NotNull List<char[]> input) {
      return new SimpleCssParser.Parser(input, SimpleCssParser.Mode.STYLE_SHEET).parse();
   }

   @NotNull
   public List<StyleProperty> parseRules(@NotNull List<char[]> input) {
      return new SimpleCssParser.Parser(input, SimpleCssParser.Mode.SINGLE_RULE).parseRules();
   }

   public enum Mode {
      STYLE_SHEET,
      SINGLE_RULE;

      // $VF: synthetic method
      private static SimpleCssParser.Mode[] $values() {
         return new SimpleCssParser.Mode[]{STYLE_SHEET, SINGLE_RULE};
      }
   }

   private static final class Parser {
      @NotNull
      private final Lexer lexer;
      @NotNull
      private final SimpleStyleSheet sheet = new SimpleStyleSheet();
      private final TokenType ruleListEndType;
      @NotNull
      private Token current = new Token(TokenType.START);

      private Parser(@NotNull List<char[]> input, SimpleCssParser.Mode mode) {
         this.ruleListEndType = mode == SimpleCssParser.Mode.SINGLE_RULE ? TokenType.EOF : TokenType.CURLY_CLOSE;
         this.lexer = new Lexer(input, mode);
      }

      private void next() {
         Token next;
         do {
            next = this.lexer.nextToken();
         } while (next.type() == TokenType.COMMENT);

         this.current = next;
      }

      private void expected(@NotNull String type) {
         SimpleCssParser.LOGGER.log(Logger.Level.WARNING, () -> MessageFormat.format("Expected ''{0}'' but got ''{1}''", type, this.current));
      }

      private void consumeOrSkipAllowedToken(TokenType type, TokenType allowedTokeToSkip) {
         if (this.current.type() != type) {
            if (this.current.type() != allowedTokeToSkip) {
               this.expected(type.toString());
               throw new ParserException();
            }
         } else {
            this.next();
         }
      }

      private void consume(TokenType type) {
         this.consumeOrSkipAllowedToken(type, null);
      }

      @NotNull
      private String consumeValue(TokenType type) {
         if (this.current.type() != type) {
            this.expected(type.toString());
            throw new ParserException();
         }

         if (this.current.data() == null) {
            throw new ParserException();
         }

         String value = Objects.requireNonNull(this.current.data());
         this.next();
         return value;
      }

      @NotNull
      private List<Token> readIdentifierList() {
         List<Token> list = new ArrayList<>();

         while (this.current.type() != TokenType.CURLY_OPEN && this.current.type() != TokenType.EOF) {
            TokenType type = this.current.type();
            if (type != TokenType.IDENTIFIER && type != TokenType.ID_NAME && type != TokenType.CLASS_NAME) {
               this.expected("identifier");
               throw new ParserException();
            }

            list.add(this.current);
            this.next();
            if (this.current.type() != TokenType.COMMA) {
               break;
            }

            this.next();
         }

         return list;
      }

      @NotNull
      private List<StyleProperty> readProperties() {
         List<StyleProperty> list = new ArrayList<>();

         while (this.current.type() != TokenType.CURLY_CLOSE && this.current.type() != TokenType.EOF) {
            String name = this.consumeValue(TokenType.IDENTIFIER);
            this.consume(TokenType.COLON);
            String value = this.consumeValue(TokenType.RAW_DATA);
            this.consumeOrSkipAllowedToken(TokenType.SEMICOLON, this.ruleListEndType);
            list.add(new StyleProperty(name, value.trim()));
         }

         return list;
      }

      private void skipToNextDefinition() {
         while (this.current.type() != TokenType.CURLY_CLOSE && this.current.type() != TokenType.EOF) {
            try {
               this.next();
            } catch (ParserException var2) {
            }
         }

         if (this.current.type() != TokenType.EOF) {
            this.current = new Token(TokenType.START);
         }
      }

      @NotNull
      List<StyleProperty> parseRules() {
         try {
            if (this.current.type() == TokenType.START) {
               this.next();
            }

            return this.readProperties();
         } catch (ParserException e) {
            return Collections.emptyList();
         }
      }

      @NotNull
      SimpleStyleSheet parse() {
         do {
            try {
               if (this.current.type() == TokenType.START) {
                  this.next();
               }

               List<Token> identifierList = this.readIdentifierList();
               this.consume(TokenType.CURLY_OPEN);
               List<StyleProperty> properties = this.readProperties();
               this.consume(TokenType.CURLY_CLOSE);

               for (Token token : identifierList) {
                  switch (token.type()) {
                     case CLASS_NAME:
                        this.sheet.addClassRules(Objects.requireNonNull(token.data()), properties);
                        break;
                     case ID_NAME:
                        this.sheet.addIdRules(Objects.requireNonNull(token.data()), properties);
                        break;
                     case IDENTIFIER:
                        this.sheet.addTagNameRules(Objects.requireNonNull(token.data()), properties);
                        break;
                     default:
                        throw new IllegalStateException("Toke = " + token);
                  }
               }
            } catch (ParserException e) {
               this.skipToNextDefinition();
            }
         } while (this.current.type() != TokenType.EOF);

         return this.sheet;
      }
   }
}

