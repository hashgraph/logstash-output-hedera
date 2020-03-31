package com.hedera;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;

import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LogstashOutputHederaTest {

    @Test
    public void testLogstashOutputHedera() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
        Map<String, Object> configValues = new HashMap<>();
        
        /*
         * Configuration for Hedera connection required, including:
         * OPERATOR_ID, OPERATOR_KEY, TOPIC_ID, NETWORK_NAME
         * You may specify MIRROR_NODE_ADDRESS, defaults to kabuto.sh if null
         */
        for (DotenvEntry e : dotenv.entries()) {
            configValues.put(e.getKey().toLowerCase(), e.getValue());
        }

        // Instantitate plugin
        Configuration config = new ConfigurationImpl(configValues);
        LogstashOutputHedera logstashOutputHedera = new LogstashOutputHedera("test-id", config, null, System.err);

        // Setup test
        int eventCount = 10;
        UUID uuid = UUID.randomUUID();
        Collection<Event> events = new ArrayList<>();
        List<String> sentMessages = new ArrayList<String>();
        List<String> receivedMessages = new ArrayList<String>();

        // Create Events with new UUID in body
        for (int i = 0; i < eventCount; i++) {
            Event event = new org.logstash.Event();
            String msg = "test " + uuid.toString() + " message " + (i + 1);
            sentMessages.add(msg);
            event.setField("message", msg);
            events.add(event);
        }

        // Set up listening on topic id
        ConsensusTopicId topicId = ConsensusTopicId.fromString((String) configValues.get("topic_id"));
        MirrorClient mirrorClient = new MirrorClient((String) configValues.get("mirror_node_address"));
        Instant startTime = Instant.now().plusMillis(100);
        Instant endTime = startTime.plusSeconds(eventCount);

        // For every message received, if it contains the UUID from above, add to recieved
        new MirrorConsensusTopicQuery()
            .setTopicId(topicId)
            .setStartTime(startTime)
            .setEndTime(endTime)
            .subscribe(mirrorClient, message -> {
                try {
                    Event e = EventEncoder.decode(message.message);
                    String m = (String) e.getField("message");
                    if (m.contains(uuid.toString())) {
                        receivedMessages.add(m);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }, Throwable::printStackTrace);

        // Actually output events
        logstashOutputHedera.output(events);

        // Wait between 1:eventCount seconds, or until eventCount messages received
        Awaitility.await()
            .atLeast(1, TimeUnit.SECONDS)
            .and().atMost(eventCount, TimeUnit.SECONDS)
            .until(receivedMessages::size, Matchers.equalTo(eventCount));
        
        // Check that sent and received messages are the same
        Assert.assertEquals(eventCount, receivedMessages.size());
        Object[] sent = sentMessages.toArray();
        Object[] received = receivedMessages.toArray();
        Arrays.sort(sent);
        Arrays.sort(received);
        Assert.assertArrayEquals(sent, received);
    }
}
