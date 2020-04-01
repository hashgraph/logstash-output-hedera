# Logstash Output Hedera

### Logstash Java Output Plugin for HCS (Hedera Consensus Service)

___
## ⚠️ Disclaimer
___

This project is currently under active development. 

Join the [Hedera discord](https://hedera.com/discord) for the latest updates and announcements.

____
### Installation
___

##### Install from RubyGems!

or

##### Setup Instructions

```
// To get just the latest logstash src to build against, use submodules
git clone --recurse-submodules git@github.com:hashgraph/logstash-output-hedera.git

// Now build local logstash
cd logstash-output-hedera/logstash
./gradlew assemble

// Set up your environment for testing, refer to env.sample
// Then you can build this project
cd ..
./gradlew build

// And you can use it locally
./gradlew gem
logstash-plugin install --no-verify --local logstash-output-logstash_output_hedera-0.0.1.gem

// Configure and run the plugin
// A sample configuration is available in hedera.conf.sample
logstash -f hedera.conf
```
___
### Configuration
___

Supports the following options:

- topic_id: (String) Your HCS topic ID
- operator_id: (String) Your operator account ID ("shard.realm.num")
- operator_key: (String) Ed25519 private key associated with the operator account
- submit_key: (String) Ed25519 private key that was used to create the topic (optional)
- network_name: (String) Which publicly available hedera network? "mainnet" or "testnet"
___

Submit Key (Optional): When a topic is _created_ with a submit key, then all messages submitted to the topic must be signed by the submit key. 

The Test environment is configured using dotenv, which is why the step for creating the .env file is required for building. The test environment also supports setting the MIRROR_NODE_ADDRESS. This is the address of a hedera mirror node that is used in the test to listen for receipt of events on HCS. By default, we use the [kabuto.sh](https://docs.kabuto.sh) mirror node. 

There is one E2E test for this plugin that:

1) Creates Some Events

2) Begins listening on the configured HCS topic

3) Sends the events using LogstashOutputHedera::output

4) Waits to receive the number of events sent

5) Checks that the sent messages and received messages match