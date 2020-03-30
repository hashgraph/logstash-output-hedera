package com.hedera;

import java.io.IOException;

import co.elastic.logstash.api.Event;

public class EventEncoder {
    public static final String encode(final Event event) throws IOException {
        System.err.println(event);
        return event.toString();
    }

    public static final Event decode(final String encodedEvent) throws IOException {
        Event event = new org.logstash.Event();
        event.setField("message", encodedEvent);
        return event;
    }
}
