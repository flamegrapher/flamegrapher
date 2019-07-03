# Flamegrapher 🔥

Flamegrapher is a frontend for the [Java Flight Recorder](http://openjdk.java.net/jeps/328) that allows you to start, dump, stop, save (locally or on S3) and download a JFR recording from the browser. Flamegrapher allows you to generate [flamegraphs](http://www.brendangregg.com/flamegraphs.html) out of the JFR recordings for methods on CPU, locks, exceptions and allocations.

Flamegrapher ships as a self-contained fat-jar. All you need to launch it is:

```
java -jar flamegrapher-1.0.0.jar
```

and then open your browser at http://localhost:8080/flames

![flamegrapher home page](https://user-images.githubusercontent.com/84847/40064225-61e8735a-585f-11e8-87da-625da1886450.png)

Once you capture some events, you will be able to dump your recording and generate the flamegraph without leaving the browser:

![cpu flamegraph](https://user-images.githubusercontent.com/84847/40064708-642dce66-5860-11e8-977a-0542678a8b04.png)

Java Flight Recorder ships with Hotspot JVM (Oracle) starting from JDK 7. It's being open-sourced and should land on OpenJDK 11.

> JFR is a commercial feature on Hotspot. It can be used for development without a license, but requires a license for production usage.

## Features

* List all JVMs compatible with JFR that are running on the same server;
* Automatically unlock commercial features and start a recording;
* Dump a JFR recording to disk;
* Save a JFR recording to remote storage (S3);
* Download remotely stored JFR recordings for opening it with Java Mission Control;
* Generate CPU, exceptions, locks, allocations in new TLAB and allocations outside TLAB flamegraphs;
* REST API.

## How it works?

Flamegrapher stands on the shoulders of giants :)

It's based on [Vertx](http://vertx.io/), so it's lightweight. It can serve dynamic and static content all shipped on the same jar, while consuming few resources on the host (can be run with a small heap). It uses [JCMD](https://dzone.com/articles/jcmd-one-jdk-command-line-tool-to-rule-them-all) to control JFR recordings (start, stop, dump). JCMD is called from Java code using [Vertx's child process extension](https://github.com/vietj/childprocess-vertx-ext). It saves to S3 using [Vertx's S3 client](https://github.com/hubrick/vertx-s3-client).

Flamegraphs are generated using Java Mission Control JFR parser, as [described by Marcus Hirt on his blog](http://hirt.se/blog/?p=920) and displayed using Martin Spier's [d3-flame-graph D3 plugin](https://github.com/spiermar/d3-flame-graph). Flamegrapher JSON writer was originally written by Isuru Perera in [jfr-flame-graph](https://github.com/chrishantha/jfr-flame-graph). OSS FTW!

Finally, flamegraphs themselves were created by [Brendan Gregg](http://www.brendangregg.com/flamegraphs.html).

## Configuration

Flamegrapher's configuration relies on [Vertx configuration module](https://vertx.io/docs/vertx-config/java/). So, you can use a json configuration file, system variable or environment variables for configuration. All settings are optional, if S3 settings are not available you will not be able use the functionality. For everything else there are sensible defaults.

* `FLAMEGRAPHER_EPHEMERAL_PORT`: If set to `true` listens on ephemeral port. Overrides `FLAMEGRAHPER_HTTP_PORT`;
* `FLAMEGRAPHER_HTTP_PORT`: HTTP listening port. Defaults to "8080";
* `FLAMEGRAPHER_HTTP_SERVER`: Network interface on which server listens. Defaults to "0.0.0.0" which means all interfaces;
* `FLAMEGRAPHER_S3_SERVER`: S3 server URL;
* `FLAMEGRAPHER_S3_REGION`: S3 region - defaults to us-east-1
* `FLAMEGRAPHER_S3_PORT`: S3 port;
* `FLAMEGRAPHER_S3_ACCESS_KEY`: Access key;
* `FLAMEGRAPHER_S3_SECRET_KEY`: Secret key;
* `FLAMEGRAPHER_S3_DUMPS_BUCKET`: S3 bucket for dumps. Defaults to "dumps";
* `FLAMEGRAPHER_S3_FLAMES_BUCKET`: S3 bucket for flames. Defaults to "flames";
* `FLAMEGRAPHER_S3_DUMPS_PREFIX`: Prefix for objects stored in the dumps bucket. Allows one bucket to be used for dumps and flames. Defaults to ""
* `FLAMEGRAPHER_S3_FLAMES_PREFIX`: Prefix for objects stored in the flames bucket. Allows one bucket to be used for dumps and flames. Defaults to ""
* `FLAMEGRAPHER_JFR_DUMP_PATH`: Base directory for saving JFR dumps locally. Defaults to "/tmp/flamegrapher";
* `JFR_SETTINGS_JDK9_PLUS`: [Optional] Custom settings for JDK 9+;
* `JFR_SETTINGS_JDK7_OR_8`: [Optional] Custom settings for JDK 7 or 8.

## Building and launching Flamegrapher

For now, we don't distribute binaries for Flamegrapher, so you have to build it yourself. The idea is that in the future we'd be able to ship a binary using the newly open-sourced JMC APIs, but for now you'll need the following:

* Oracle JDK 10+
* Maven 3.x (tested with 3.5.3)
* Git

Steps:

* Clone this repository locally
* Make sure you have the Oracle JDK 10+ set (you can check that with `mvn -v`)
* Run the `install-mc-jars.sh` to get the JMC jars from your Oracle JDK installation (they're not yet available on Maven central yet). This script has been tested on Mac and Windows.
* Run `mvn clean package` to build Flamegrapher
* Your fat jar should be available under `target/flamegrapher-[version].jar`

Launching:

Assuming that you're in the same directory from where you built the jar, you can launch:

```bash
java -jar target/flamegrapher-[version].jar
```

## To know more

We're doing a [presentation at Riviera Dev](http://rivieradev.fr/session/312) and slides should be available soon. We will explain
in more details how to read exceptions and locks flamegraphs, for example.