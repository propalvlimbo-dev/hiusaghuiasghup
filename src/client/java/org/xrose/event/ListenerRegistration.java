package org.xrose.event;

record ListenerRegistration(Class<? extends Event> eventType, RegisteredListener listener) {
}

