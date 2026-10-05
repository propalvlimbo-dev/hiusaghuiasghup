package com.github.weisj.jsvg.geometry.path;

import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.util.ParserBase;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class PathParser extends ParserBase {
   private static final Logger LOGGER = LogFactory.createLogger(PathParser.class);
   private char currentCommand;

   public PathParser(@NotNull String input) {
      super(input, 0);
   }

   private boolean isCommandChar(char c) {
      return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z';
   }

   @Override
   public float nextFloat() throws NumberFormatException {
      float f = super.nextFloat();
      this.consumeWhiteSpaceOrSeparator();
      return f;
   }

   private boolean nextFlag() {
      char c = this.peek();
      this.consume();
      this.consumeWhiteSpaceOrSeparator();
      if (c == '1') {
         return true;
      } else if (c == '0') {
         return false;
      } else {
         throw new IllegalStateException("Invalid flag value '" + c + "' " + this.currentLocation());
      }
   }

   @NotNull
   public BezierPathCommand parseMeshCommand() {
      char peekChar = this.peek();
      this.currentCommand = 'z';
      if (this.isCommandChar(peekChar)) {
         this.consume();
         this.currentCommand = peekChar;
      }

      this.consumeWhiteSpaceOrSeparator();
      switch (this.currentCommand) {
         case 'C':
            return new CubicBezierCommand(
               false, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloatOrUnspecified(), this.nextFloatOrUnspecified()
            );
         case 'L':
            return new LineToBezier(false, this.nextFloatOrUnspecified(), this.nextFloatOrUnspecified());
         case 'c':
            return new CubicBezierCommand(
               true, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloatOrUnspecified(), this.nextFloatOrUnspecified()
            );
         case 'l':
            return new LineToBezier(true, this.nextFloatOrUnspecified(), this.nextFloatOrUnspecified());
         default:
            throw new IllegalStateException("Only commands c C l L allowed");
      }
   }

   public PathCommand[] parsePathCommand() {
      if ("none".equals(this.input)) {
         return new PathCommand[0];
      }

      List<PathCommand> commands = new ArrayList<>();

      try {
         this.parsePathCommandInto(commands);
      } catch (Exception e) {
         LOGGER.log(Logger.Level.INFO, "Error parsing path command: " + e.getMessage());
      }

      return commands.toArray(new PathCommand[0]);
   }

   private void parsePathCommandInto(List<PathCommand> commands) {
      this.currentCommand = 'Z';

      while (this.hasNext()) {
         char peekChar = this.peek();
         if (this.isCommandChar(peekChar)) {
            this.consume();
            this.currentCommand = peekChar;
         }

         this.consumeWhiteSpaceOrSeparator();
         if (this.currentCommand != 'M' && this.currentCommand != 'm' && commands.isEmpty()) {
            throw new IllegalArgumentException("Path must start with a move command " + this.currentLocation());
         }

         PathCommand cmd;
         switch (this.currentCommand) {
            case 'A':
               cmd = new Arc(false, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFlag(), this.nextFlag(), this.nextFloat(), this.nextFloat());
               break;
            case 'B':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'I':
            case 'J':
            case 'K':
            case 'N':
            case 'O':
            case 'P':
            case 'R':
            case 'U':
            case 'W':
            case 'X':
            case 'Y':
            case '[':
            case '\\':
            case ']':
            case '^':
            case '_':
            case '`':
            case 'b':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'i':
            case 'j':
            case 'k':
            case 'n':
            case 'o':
            case 'p':
            case 'r':
            case 'u':
            case 'w':
            case 'x':
            case 'y':
            default:
               throw new IllegalArgumentException("Invalid path element " + this.currentCommand + this.currentLocation());
            case 'C':
               cmd = new Cubic(false, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 'H':
               cmd = new Horizontal(false, this.nextFloat());
               break;
            case 'L':
               cmd = new LineTo(false, this.nextFloat(), this.nextFloat());
               break;
            case 'M':
               cmd = new MoveTo(false, this.nextFloat(), this.nextFloat());
               this.currentCommand = 'L';
               break;
            case 'Q':
               cmd = new Quadratic(false, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 'S':
               cmd = new CubicSmooth(false, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 'T':
               cmd = new QuadraticSmooth(false, this.nextFloat(), this.nextFloat());
               break;
            case 'V':
               cmd = new Vertical(false, this.nextFloat());
               break;
            case 'Z':
            case 'z':
               cmd = new Terminal();
               break;
            case 'a':
               cmd = new Arc(true, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFlag(), this.nextFlag(), this.nextFloat(), this.nextFloat());
               break;
            case 'c':
               cmd = new Cubic(true, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 'h':
               cmd = new Horizontal(true, this.nextFloat());
               break;
            case 'l':
               cmd = new LineTo(true, this.nextFloat(), this.nextFloat());
               break;
            case 'm':
               cmd = new MoveTo(true, this.nextFloat(), this.nextFloat());
               this.currentCommand = 'l';
               break;
            case 'q':
               cmd = new Quadratic(true, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 's':
               cmd = new CubicSmooth(true, this.nextFloat(), this.nextFloat(), this.nextFloat(), this.nextFloat());
               break;
            case 't':
               cmd = new QuadraticSmooth(true, this.nextFloat(), this.nextFloat());
               break;
            case 'v':
               cmd = new Vertical(true, this.nextFloat());
         }

         commands.add(cmd);
      }
   }
}

