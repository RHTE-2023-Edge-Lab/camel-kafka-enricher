# camel-kafka-enricher Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```sh
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Configuration

In the **application.properties**:

```ini
enricher.topic.TOPIC.direction={in,out}
enricher.topic.TOPIC.warehouse={par,bru,lon,lis,ath,sto,var,dub,buc,brn}
enricher.sink-topic=location-records
enricher.kafka-brokers=localhost:9092
```

## Packaging and running the application

The application can be packaged using:

```sh
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```sh
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```sh
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```sh
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/camel-kafka-enricher-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Development environment

Start the Kafka broker and create two topics.

```sh
podman-compose up -d
podman exec broker kafka-topics --bootstrap-server broker:9092 --create --topic warehouse-par-in
podman exec broker kafka-topics --bootstrap-server broker:9092 --create --topic warehouse-par-out
podman exec broker kafka-topics --bootstrap-server broker:9092 --create --topic warehouse-lon-in
podman exec broker kafka-topics --bootstrap-server broker:9092 --create --topic warehouse-lon-out
podman exec broker kafka-topics --bootstrap-server broker:9092 --create --topic location-records
```

Create **kcat.conf** with the following content:

```ini
bootstrap.servers=localhost:9092
security.protocol=PLAINTEXT

# Best practice for higher availability in librdkafka clients prior to 1.7
session.timeout.ms=45000
```

Start the following commands in a separate terminal.

```sh
kcat -b localhost:9092 -C -F kcat.conf -t location-records -f "%k => %s\n"
```

Send events to each incoming topic:

```sh
kcat -b localhost:9092 -P -F kcat.conf -t warehouse-par-in -k 11:22:33:44 <<EOF
{"parcelNumber":"11:22:33:44","timestamp":$(date +%s -d "now")}
EOF
kcat -b localhost:9092 -P -F kcat.conf -t warehouse-par-out -k 11:22:33:44 <<EOF
{"parcelNumber":"11:22:33:44","timestamp":$(date +%s -d "now")}
EOF
kcat -b localhost:9092 -P -F kcat.conf -t warehouse-lon-in -k 11:22:33:44 <<EOF
{"parcelNumber":"11:22:33:44","timestamp":$(date +%s -d "now")}
EOF
kcat -b localhost:9092 -P -F kcat.conf -t warehouse-lon-out -k 11:22:33:44 <<EOF
{"parcelNumber":"11:22:33:44","timestamp":$(date +%s -d "now")}
EOF
```

## Related Guides

- Camel Kafka ([guide](https://camel.apache.org/camel-quarkus/latest/reference/extensions/kafka.html)): Sent and receive messages to/from an Apache Kafka broker
