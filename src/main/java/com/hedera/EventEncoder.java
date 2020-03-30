package com.hedera;

import java.io.IOException;

import co.elastic.logstash.api.Event;

public class EventEncoder {
    public static final byte[] encode(final Event event) throws IOException {
        return ((org.logstash.Event) event).serialize();
    }

    public static final Event decode(final byte[] encodedEvent) throws IOException {
        return org.logstash.Event.deserialize(encodedEvent);
    }
}
