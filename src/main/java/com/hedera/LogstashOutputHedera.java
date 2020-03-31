package com.hedera;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.HederaThrowable;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.ConsensusMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;

// Class name must match plugin name (also in build.gradle)
@LogstashPlugin(name = "logstash_output_hedera")
public class LogstashOutputHedera implements Output {
    public static final PluginConfigSpec<String> OPERATOR_ID_CONFIG = PluginConfigSpec.stringSetting("operator_id");
    public static final PluginConfigSpec<String> OPERATOR_KEY_CONFIG = PluginConfigSpec.stringSetting("operator_key");
    public static final PluginConfigSpec<String> TOPIC_ID_CONFIG = PluginConfigSpec.stringSetting("topic_id");
    public static final PluginConfigSpec<String> NETWORK_NAME_CONFIG = PluginConfigSpec.stringSetting("network_name");
    public static final PluginConfigSpec<String> SUBMIT_KEY_CONFIG = PluginConfigSpec.stringSetting("submit_key", null);

    // Logstash
    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;
    private final PrintStream printStream;

    // HCS
    private final AccountId operatorId;
    private final Ed25519PrivateKey operatorKey;
    private final ConsensusTopicId topicId;
    private final String networkName;
    private final Ed25519PrivateKey submitKey;
    private final Client hapiClient;

    // All plugins must provide a constructor that accepts id, Configuration, and
    // Context
    public LogstashOutputHedera(final String id, final Configuration configuration, final Context context) {
        this(id, configuration, context, System.out);
    }

    private final Client createClient() {
        Client client = null;

        if (this.isTestnet()) {
            client = Client.forTestnet();
        } else if (!this.isTestnet()) {
            client = Client.forMainnet();
        }

        if (client != null) {
            client.setOperator(this.operatorId, this.operatorKey);
        }

        return client;
    }

    private final boolean isTestnet() {
        return this.networkName.contains("test") || this.networkName.contains("testnet");
    }

    LogstashOutputHedera(final String id, final Configuration config, final Context context,
            OutputStream targetStream) {
        // Validate configuration settings here
        this.id = id;
        this.printStream = new PrintStream(targetStream);
        this.operatorId = AccountId.fromString(config.get(OPERATOR_ID_CONFIG));
        this.operatorKey = Ed25519PrivateKey.fromString(config.get(OPERATOR_KEY_CONFIG));
        this.topicId = ConsensusTopicId.fromString(config.get(TOPIC_ID_CONFIG));
        this.networkName = config.get(NETWORK_NAME_CONFIG);
        this.submitKey = config.get(SUBMIT_KEY_CONFIG) == null ? null
                : Ed25519PrivateKey.fromString(config.get(SUBMIT_KEY_CONFIG));
        this.hapiClient = createClient();
    }

    private void handleHederaMessage(Object response) {
        /* Do nothing */}

    private void handleHederaError(HederaThrowable throwable) throws HederaStatusException, HederaNetworkException {
        if (throwable instanceof HederaStatusException) {
            throw (HederaStatusException) throwable;
        } else if (throwable instanceof HederaNetworkException) {
            throw (HederaNetworkException) throwable;
        }
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();

        while (z.hasNext() && !this.stopped) {
            Event event = z.next();
            byte[] encodedEvent = null;

            try {
                encodedEvent = EventEncoder.encode(event);
            } catch (IOException e) {
                e.printStackTrace(this.printStream);
                continue;
            }

            Transaction consensusTransaction = new ConsensusMessageSubmitTransaction().setTopicId(this.topicId)
                    .setMessage(encodedEvent).build(this.hapiClient);

            if (this.submitKey != null) {
                consensusTransaction.sign(this.submitKey);
            }

            consensusTransaction.executeAsync(this.hapiClient, this::handleHederaMessage, error -> {
                try {
                    handleHederaError(error);
                } catch (HederaNetworkException | HederaStatusException e) {
                    e.printStackTrace(this.printStream);
                }
            });
        }
    }

    @Override
    public void stop() {
        try {
            this.hapiClient.close();
        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace(this.printStream);
        }

        this.stopped = true;
        this.done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        this.done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // Should return a list of all configuration options for this plugin
        List<PluginConfigSpec<?>> configs = new ArrayList<PluginConfigSpec<?>>();
        configs.add(TOPIC_ID_CONFIG);
        configs.add(OPERATOR_ID_CONFIG);
        configs.add(OPERATOR_KEY_CONFIG);
        configs.add(SUBMIT_KEY_CONFIG);
        configs.add(NETWORK_NAME_CONFIG);
        return configs;
    }

    @Override
    public String getId() {
        return id;
    }
}
