# Logstash Plugin Hedera

## ⚠️ Disclaimer

This project is actively under development and not recommended for production use. 
Join the [Hedera discord](https://hedera.com/discord) for the latest updates and announcements.

This is a Java plugin for [Logstash](https://github.com/elastic/logstash).

It is fully free and fully open source. The license is Apache 2.0, meaning you are free to use it however you want.

The documentation for Logstash Java plugins is available [here](https://www.elastic.co/guide/en/logstash/6.7/contributing-java-plugin.html).

This plugin is an _output_ plugin that sends messages to a HCS (Hedera Consensus Service) topic. 

When cloning this repository, use `--recurse-submodules` to get the required (for development) logstash source

You may have problems with getting vs code to correctly resolve imports. I do not know how to fix this, only that it does eventually go away. 

```
git clone --recurse-submodules <this>
cd logstash
./gradlew assemble
cd ..
./gradlew build
```
