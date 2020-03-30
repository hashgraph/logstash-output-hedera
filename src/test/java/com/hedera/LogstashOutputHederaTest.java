package com.hedera;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;

import com.google.gson.JsonParseException;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
        for (DotenvEntry e : dotenv.entries()) {
            configValues.put(e.getKey().toLowerCase(), e.getValue());
        }

        Configuration config = new ConfigurationImpl(configValues);
        LogstashOutputHedera logstashOutputHedera = new LogstashOutputHedera("test-id", config, null, System.err);

        int eventCount = 10;
        UUID uuid = UUID.randomUUID();
        Collection<Event> events = new ArrayList<>();
        List<String> sentMessages = new ArrayList<String>();
        List<String> receivedMessages = new ArrayList<String>();

        for (int i = 0; i < eventCount; i++) {
            Event event = new org.logstash.Event();
            String msg = "test " + uuid.toString() + " message " + i;
            sentMessages.add(msg);
            event.setField("message", msg);
            events.add(event);
        }

        ConsensusTopicId topicId = ConsensusTopicId.fromString((String) configValues.get("topic_id"));
        Instant startTime = Instant.now();
        MirrorClient mirrorClient = new MirrorClient("api.testnet.kabuto.sh:50211");

        new MirrorConsensusTopicQuery().setTopicId(topicId).setStartTime(startTime).subscribe(mirrorClient, message -> {
            try {
                Event event = EventEncoder.decode(message.message.toString());
                String msg = (String) event.getField("message");
                receivedMessages.add(msg);
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            }, Throwable::printStackTrace);

        logstashOutputHedera.output(events);
        Assert.assertEquals(sentMessages.size(), eventCount);
        Awaitility.await().atLeast(1, TimeUnit.SECONDS).and().atMost(10, TimeUnit.SECONDS).until(receivedMessages::size, Matchers.equalTo(10));
        Assert.assertArrayEquals(sentMessages.toArray(), receivedMessages.toArray());
    }
}
