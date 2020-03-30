## ⚠️ Disclaimer

This project is actively under development and not recommended for production use. 
Join the [Hedera discord](https://hedera.com/discord) for the latest updates and announcements.

## Logstash Output Hedera

This is a Java plugin for [Logstash](https://github.com/elastic/logstash).

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

The documentation for Logstash Java plugins is available [here](https://www.elastic.co/guide/en/logstash/7.2/contributing-java-plugin.html).

This plugin is an _output_ plugin that sends messages to a HCS (Hedera Consensus Service) topic. 

### Configuration

You will need a .env file (or set environment variables) containing information for constructing a hedera client and for connecting to the hedera network. 
The available configuration options are:

- TOPIC_ID: (String) the target HCS topic, in format of "shard.realm.num"
- OPERATOR_ID: (String) the operator account id in the format "shard.realm.num"
- OPERATOR_KEY: (String) Ed25519 private key associated with the operator account
- SUBMIT_KEY: (String) Ed25519 private key used to create the topic (optional), which is required to submit
- NETWORK_NAME: (String) Either "mainnet" or "testnet" (no custom network support)
- MIRROR_NODE_ADDRESS: (String) Address of Hedera mirror node as URI

If you leave MIRROR_NODE_ADDRESS unconfigured, then [kabuto.sh](https://docs.kabuto.sh)'s mirror's will be used.

### Development

When cloning this repository, use `--recurse-submodules` to get the required (for development) logstash source

You may have problems with getting vs code to correctly resolve imports. I do not know how to fix this, only that it does eventually go away.

The Unit test for this plugin is more of an E2E test, it creates 10 fake events with a UUID in their body, sends them to HCS, listens to HCS, then checks that
the sent messages and received messages match.

```
git clone --recurse-submodules <this>
cd <this>
cd logstash
./gradlew assemble
cd ..
./gradlew build
```
