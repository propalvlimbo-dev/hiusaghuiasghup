package com.github.weisj.jsvg.parser;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import org.jetbrains.annotations.NotNull;

public interface XMLInput {
   @NotNull
   XMLEventReader createReader() throws XMLStreamException;
}
