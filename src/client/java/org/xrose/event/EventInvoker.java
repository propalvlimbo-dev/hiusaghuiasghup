package org.xrose.event;

@FunctionalInterface
public interface EventInvoker {
   void invoke(Object var1, Event var2);
}
