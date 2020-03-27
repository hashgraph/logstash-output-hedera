package llc.launchbadge;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;

import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaStatusException;
import com.hedera.hashgraph.sdk.Transaction;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.ConsensusMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hashgraph.sdk.mirror.MirrorClient;
import com.hedera.hashgraph.sdk.mirror.MirrorConsensusTopicQuery;

// Class name must match plugin name (also in build.gradle)
@LogstashPlugin(name = "logstash_output_hcs")
public class LogstashOutputHcs implements Output {
    public static final PluginConfigSpec<String> OPERATOR_ID_CONFIG = PluginConfigSpec.stringSetting("operator_id");
    public static final PluginConfigSpec<String> OPERATOR_KEY_CONFIG = PluginConfigSpec.stringSetting("operator_key");
    public static final PluginConfigSpec<String> TOPIC_ID_CONFIG = PluginConfigSpec.stringSetting("topic_id");
    public static final PluginConfigSpec<String> NETWORK_NAME_CONFIG = PluginConfigSpec.stringSetting("network_name");
    public static final PluginConfigSpec<String> MIRROR_NODE_ADDRESS_CONFIG = PluginConfigSpec
            .stringSetting("mirror_node_address", null);
    public static final PluginConfigSpec<String> SUBMIT_KEY_CONFIG = PluginConfigSpec.stringSetting("submit_key", null);

    // Logstash
    private final String id;
    private final CountDownLatch done = new CountDownLatch(1);
    private volatile boolean stopped = false;

    // HCS
    private final AccountId operatorId;
    private final Ed25519PrivateKey operatorKey;
    private final ConsensusTopicId topicId;
    private final String networkName;
    private final Ed25519PrivateKey submitKey;
    private final Client hapiClient;
    private final MirrorClient mirrorNodeClient;
    private final String mirrorNodeAddress;

    // All plugins must provide a constructor that accepts id, Configuration, and Context
    public LogstashOutputHcs(final String id, final Configuration configuration, final Context context) {
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

    private final MirrorClient createMirrorNodeClient() {
        MirrorClient client = null;
        if (this.mirrorNodeAddress == null) {
            if (isTestnet()) {
                client = new MirrorClient("api.testnet.kabuto.sh:50211");
            } else if (!isTestnet()) {
                client = new MirrorClient("api.kabuto.sh:50211");
            }
        } else {
            client = new MirrorClient(this.mirrorNodeAddress);
        }

        return client;
    }

    private final boolean isTestnet() {
        return this.networkName.contains("test") || this.networkName.contains("testnet");
    }

    private final void checkTopic() {
        new MirrorConsensusTopicQuery()
            .setTopicId(this.topicId)
            .setStartTime(Instant.ofEpochSecond(0))
            .subscribe(this.mirrorNodeClient, null, null);
    }

    LogstashOutputHcs(final String id, final Configuration config, final Context context, OutputStream targetStream) {
        // Validate configuration settings here
        this.id = id;
        this.operatorId = AccountId.fromString(config.get(OPERATOR_ID_CONFIG));
        this.operatorKey = Ed25519PrivateKey.fromString(config.get(OPERATOR_KEY_CONFIG));
        this.topicId = ConsensusTopicId.fromString(config.get(TOPIC_ID_CONFIG));
        this.networkName = config.get(NETWORK_NAME_CONFIG);
        this.submitKey = Ed25519PrivateKey.fromString(config.get(SUBMIT_KEY_CONFIG));
        this.hapiClient = createClient();
        this.mirrorNodeAddress = config.get(MIRROR_NODE_ADDRESS_CONFIG);
        this.mirrorNodeClient = createMirrorNodeClient();
        checkTopic();
    }

    @Override
    public void output(final Collection<Event> events) {
        Iterator<Event> z = events.iterator();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        while (z.hasNext() && !this.stopped) {
            Event event = z.next();
            String encodedEvent = gson.toJson(event);
            
            try {
                Transaction consensusTransaction = new ConsensusMessageSubmitTransaction()
                    .setTopicId(this.topicId)
                    .setMessage(encodedEvent)
                    .build(this.hapiClient);
            
                if (this.submitKey != null) {
                    consensusTransaction.sign(this.submitKey);
                }

                consensusTransaction.execute(this.hapiClient).getReceipt(this.hapiClient);
            } catch (HederaStatusException e) {
                e.printStackTrace();
            }         
        }
    }

    @Override
    public void stop() {
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
