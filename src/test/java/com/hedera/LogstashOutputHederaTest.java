package com.hedera;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;
import com.hedera.LogstashOutputHedera;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LogstashOutputHederaTest {

    @Test
    public void testLogstashOutputHcs() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
        Map<String, Object> configValues = new HashMap<>();
        for (DotenvEntry e : dotenv.entries()) {
            System.out.println(e.getKey() + ": " + e.getValue());
            configValues.put(e.getKey(), e.getValue());
        }

        Configuration config = new ConfigurationImpl(configValues);
        LogstashOutputHedera output = new LogstashOutputHedera("test-id", config, null);

        Collection<Event> events = new ArrayList<>();
        // Add events here

        output.output(events);
        // Subscribe to HCS with parameters from above and check that events were received
    }
}
