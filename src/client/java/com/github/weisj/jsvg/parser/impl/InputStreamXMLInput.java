package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.parser.XMLInput;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.jetbrains.annotations.NotNull;

public final class InputStreamXMLInput implements XMLInput {
   @NotNull
   private final XMLInputFactory xmlInputFactory;
   @NotNull
   private final InputStream inputStream;

   public InputStreamXMLInput(@NotNull XMLInputFactory xmlInputFactory, @NotNull InputStream inputStream) {
      this.xmlInputFactory = xmlInputFactory;
      this.inputStream = inputStream;
   }

   @NotNull
   @Override
   public XMLEventReader createReader() throws XMLStreamException {
      return this.xmlInputFactory.createXMLEventReader(this.inputStream);
   }
}

