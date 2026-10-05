package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import org.jetbrains.annotations.NotNull;

final class CharacterDataParser {
   private static final Logger LOGGER = LogFactory.createLogger(CharacterDataParser.class);
   private CharacterDataParser.State state = CharacterDataParser.State.SEGMENT_START;
   private StringBuilder buffer = new StringBuilder();
   private boolean hadAnyContent = false;
   private char[] data;
   private int begin;
   private int end;

   public void append(char[] ch, int offset, int length) {
      if (length != 0) {
         this.hadAnyContent = true;
         LOGGER.log(Logger.Level.DEBUG, () -> String.format("Append: [%s]", new String(ch, offset, length).replace("\n", "\\n")));
         this.data = ch;
         this.begin = offset;
         this.end = offset + length;
         if (isSegmentBreak(this.data[this.begin])) {
            int segmentBreaks = this.trimLeadingWhiteSpace();
            if (this.state == CharacterDataParser.State.SEGMENT_BREAK) {
               segmentBreaks++;
            }

            if (this.begin > offset && segmentBreaks > 1) {
               this.begin--;
               this.data[this.begin] = ' ';
               if (this.state == CharacterDataParser.State.CHARACTER || this.state == CharacterDataParser.State.SEGMENT_BREAK) {
                  this.state = CharacterDataParser.State.WHITESPACE_AFTER_CHAR;
               }
            }
         }

         int segmentBreaks = this.trimTrailingWhiteSpace();
         if (this.end < offset + length) {
            this.data[this.end] = (char)(segmentBreaks > 0 ? 10 : 32);
            this.end++;
         }

         if (this.begin < this.end) {
            LOGGER.log(Logger.Level.DEBUG, () -> String.format("Portion: [%s]", new String(ch, this.begin, this.end - this.begin).replace("\n", "\\n")));
            this.buffer.ensureCapacity(this.buffer.length() + this.end - this.begin);
            this.appendData();
         }
      }
   }

   private void appendData() {
      int initialOffset = this.begin;

      while (this.begin < this.end) {
         char c = this.data[this.begin];
         boolean segmentBreak = isSegmentBreak(c);
         boolean whiteSpace = isWhitespace(c);
         if (!segmentBreak && !whiteSpace) {
            if (this.state == CharacterDataParser.State.WHITESPACE_AFTER_CHAR || this.state.isVisualSpace && this.begin > initialOffset) {
               this.buffer.append(' ');
            }

            this.state = CharacterDataParser.State.CHARACTER;
            this.buffer.append(c);
         } else if (whiteSpace) {
            switch (this.state) {
               case SEGMENT_BREAK:
               case WHITESPACE_AFTER_SEGMENT_BREAK:
                  this.state = CharacterDataParser.State.WHITESPACE_AFTER_SEGMENT_BREAK;
                  break;
               case WHITESPACE_AFTER_CHAR:
               case CHARACTER:
                  this.state = CharacterDataParser.State.WHITESPACE_AFTER_CHAR;
            }
         } else {
            this.state = CharacterDataParser.State.SEGMENT_BREAK;
         }

         this.begin++;
      }
   }

   public boolean canFlush(boolean dueToSegmentBreak) {
      return this.state == CharacterDataParser.State.SEGMENT_START && !this.hadAnyContent ? false : dueToSegmentBreak || this.buffer.length() > 0;
   }

   @NotNull
   public String flush(boolean dueToSegmentBreak) {
      this.hadAnyContent = false;
      if (dueToSegmentBreak && this.state != CharacterDataParser.State.CHARACTER) {
         this.buffer.append(' ');
      }

      if (dueToSegmentBreak) {
         this.state = CharacterDataParser.State.SEGMENT_BREAK;
      }

      String result = this.buffer.toString();
      LOGGER.log(Logger.Level.DEBUG, () -> String.format("Flush segBreak=%s[%s]", dueToSegmentBreak, this.buffer));
      this.buffer = new StringBuilder();
      return result;
   }

   private int trimLeadingWhiteSpace() {
      int segmentBreakCount = 0;

      while (this.begin < this.end) {
         if (isSegmentBreak(this.data[this.begin])) {
            segmentBreakCount++;
            this.begin++;
         } else {
            if (!isWhitespace(this.data[this.begin])) {
               break;
            }

            this.begin++;
         }
      }

      return segmentBreakCount;
   }

   private int trimTrailingWhiteSpace() {
      int segmentBreakCount = 0;

      while (this.begin < this.end) {
         if (isSegmentBreak(this.data[this.end - 1])) {
            segmentBreakCount++;
            this.end--;
         } else {
            if (!isWhitespace(this.data[this.end - 1])) {
               break;
            }

            this.end--;
         }
      }

      return segmentBreakCount;
   }

   private static boolean isSegmentBreak(char c) {
      return c == '\n' || c == '\r';
   }

   private static boolean isWhitespace(char c) {
      return c == ' ' || c == '\t';
   }

   private enum State {
      SEGMENT_START(false),
      SEGMENT_BREAK(true),
      WHITESPACE_AFTER_CHAR(true),
      WHITESPACE_AFTER_SEGMENT_BREAK(true),
      CHARACTER(false);

      private final boolean isVisualSpace;

      State(boolean isVisualSpace) {
         this.isVisualSpace = isVisualSpace;
      }

      // $VF: synthetic method
      private static CharacterDataParser.State[] $values() {
         return new CharacterDataParser.State[]{SEGMENT_START, SEGMENT_BREAK, WHITESPACE_AFTER_CHAR, WHITESPACE_AFTER_SEGMENT_BREAK, CHARACTER};
      }
   }
}

