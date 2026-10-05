package com.github.weisj.jsvg.parser.impl;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.logging.Logger;
import com.github.weisj.jsvg.logging.impl.LogFactory;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.XMLInput;
import com.github.weisj.jsvg.util.supplier.LazySupplier;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class StaxSVGLoader {
   private static final Logger LOGGER = LogFactory.createLogger(StaxSVGLoader.class);
   private static final String SVG_NAMESPACE_URI = "http://www.w3.org/2000/svg";
   private static final String XLINK_NAMESPACE_URI = "http://www.w3.org/1999/xlink";
   @NotNull
   private static final NodeSupplier NODE_SUPPLIER = new NodeSupplier();
   @NotNull
   private final Supplier<XMLInputFactory> xmlInputFactory = new LazySupplier<>(() -> {
      XMLInputFactory factory = XMLInputFactory.newFactory();
      factory.setProperty("javax.xml.stream.supportDTD", false);
      factory.setProperty("javax.xml.stream.isReplacingEntityReferences", false);
      factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
      return factory;
   });

   @Nullable
   SVGDocumentBuilder parse(@NotNull InputStream inputStream, @Nullable URI xmlBase, @NotNull LoaderContext loaderContext) throws XMLStreamException {
      return this.parse(this.createXMLInput(inputStream), xmlBase, loaderContext);
   }

   @Nullable
   SVGDocumentBuilder parse(@NotNull XMLInput xmlInput, @Nullable URI xmlBase, @NotNull LoaderContext loaderContext) throws XMLStreamException {
      XMLEventReader reader = null;

      try {
         reader = xmlInput.createReader();
         SVGDocumentBuilder builder = new SVGDocumentBuilder(xmlBase, loaderContext, NODE_SUPPLIER);

         while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            switch (event.getEventType()) {
               case 1:
                  StartElement element = event.asStartElement();
                  String uri = element.getName().getNamespaceURI();
                  if (uri != null && !uri.isEmpty() && !"http://www.w3.org/2000/svg".equals(uri)) {
                     skipElement(reader);
                  } else {
                     Map<String, String> attributes = new HashMap<>();
                     Iterator<Attribute> attrs = element.getAttributes();

                     while (attrs.hasNext()) {
                        Attribute attr = attrs.next();
                        attributes.put(qualifiedName(attr.getName()), attr.getValue().trim());
                     }

                     if (!builder.startElement(qualifiedName(element.getName(), StaxSVGLoader.MakeLowerCase.YES), attributes)) {
                        skipElement(reader);
                     }
                  }
                  break;
               case 2:
                  builder.endElement(qualifiedName(event.asEndElement().getName(), StaxSVGLoader.MakeLowerCase.YES));
               case 3:
               case 5:
               case 6:
               case 9:
               case 10:
               case 11:
               case 13:
               case 14:
               case 15:
               default:
                  break;
               case 4:
               case 12:
                  char[] data = event.asCharacters().getData().toCharArray();
                  builder.addTextContent(data, 0, data.length);
                  break;
               case 7:
                  builder.startDocument();
                  break;
               case 8:
                  builder.endDocument();
            }
         }

         return builder;
      } catch (XMLStreamException e) {
         LOGGER.log(Logger.Level.WARNING, "Error while parsing SVG.", e);
      } finally {
         if (reader != null) {
            reader.close();
         }
      }

      return null;
   }

   @Nullable
   public SVGDocument load(@NotNull XMLInput xmlInput, @Nullable URI xmlBase, @NotNull LoaderContext loaderContext) throws XMLStreamException {
      SVGDocumentBuilder builder = this.parse(xmlInput, xmlBase, loaderContext);
      return builder == null ? null : builder.build();
   }

   private static void skipElement(@NotNull XMLEventReader reader) throws XMLStreamException {
      int elementCount = 1;

      while (reader.hasNext()) {
         XMLEvent event = reader.nextEvent();
         if (event.isStartElement()) {
            elementCount++;
         } else if (event.isEndElement()) {
            elementCount--;
         }

         if (elementCount == 0) {
            return;
         }
      }
   }

   @NotNull
   public XMLInput createXMLInput(@NotNull InputStream inputStream) {
      return new InputStreamXMLInput(this.xmlInputFactory.get(), inputStream);
   }

   @NotNull
   private static String qualifiedName(@NotNull QName name, StaxSVGLoader.MakeLowerCase makeLowerCase) {
      String qName = qualifiedNameImpl(name);
      return makeLowerCase == StaxSVGLoader.MakeLowerCase.YES ? qName.toLowerCase(Locale.ROOT) : qName;
   }

   @NotNull
   private static String qualifiedName(@NotNull QName name) {
      return qualifiedName(name, StaxSVGLoader.MakeLowerCase.NO);
   }

   @NotNull
   private static String qualifiedNameImpl(@NotNull QName name) {
      String prefix = name.getPrefix();
      String localName = name.getLocalPart();
      if (prefix == null) {
         return localName;
      } else if (prefix.isEmpty()) {
         return localName;
      } else if ("http://www.w3.org/2000/svg".equals(name.getNamespaceURI())) {
         return localName;
      } else {
         return "http://www.w3.org/1999/xlink".equals(name.getNamespaceURI()) ? "xlink:" + localName : prefix + ":" + localName;
      }
   }

   private enum MakeLowerCase {
      YES,
      NO;

      // $VF: synthetic method
      private static StaxSVGLoader.MakeLowerCase[] $values() {
         return new StaxSVGLoader.MakeLowerCase[]{YES, NO};
      }
   }
}

