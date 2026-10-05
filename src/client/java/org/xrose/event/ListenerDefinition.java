package org.xrose.event;

record ListenerDefinition(Class<? extends Event> eventType, int priority, String methodName, EventInvoker invoker) {
}

