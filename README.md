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
enricher.topic.TOPIC.warehouse={PAR,BRU,LON,LIS,ATH,STO,VAR,DUB,BUC,BRN}
enricher.sink-topic=location-records
enricher.kafka-brokers=localhost:9092
enricher.kafka.security-protocol=PLAINTEXT
enricher.kafka.sasl-mechanism=PLAIN
enricher.kafka.jaas-config="org.apache.kafka.common.security.plain.PlainLoginModule required username='john' password='secret';"
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

## Container image

```sh
APP_VERSION="$(./mvnw -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)"
podman build -f src/main/docker/Dockerfile.jvm -t quay.io/rhte2023edgelab/camel-kafka-enricher:latest .
podman tag quay.io/rhte2023edgelab/camel-kafka-enricher:latest quay.io/rhte2023edgelab/camel-kafka-enricher:$APP_VERSION
podman push quay.io/rhte2023edgelab/camel-kafka-enricher:$APP_VERSION
podman push quay.io/rhte2023edgelab/camel-kafka-enricher:latest
```

## Deploy in Kubernetes

Create a project named "headquarter".

Install the **Red Hat Integration - AMQ Streams** operator

Create a Kafka resource.

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: headquarter
spec:
  kafka:
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
      inter.broker.protocol.version: '3.2'
    storage:
      type: ephemeral
    listeners:
      - authentication:
          type: scram-sha-512
        name: plain
        port: 9092
        type: internal
        tls: false
      - authentication:
          type: scram-sha-512
        name: tls
        port: 9093
        type: route
        tls: true
    version: 3.2.3
    replicas: 3
  entityOperator:
    topicOperator: {}
    userOperator: {}
  zookeeper:
    storage:
      type: ephemeral
    replicas: 3
```

Deploy all Kubernetes manifests.

```sh
kubectl apply -f k8s
```

Create **kcat-hq.conf** as follow:

```ini
# Required connection configs for Kafka producer, consumer, and admin
bootstrap.servers=headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443
ssl.ca.location=/home/nmasse/tmp/headquarter.pem
security.protocol=SASL_SSL
sasl.mechanisms=SCRAM-SHA-512
sasl.username=camel-kafka-enricher
sasl.password=s3cr3t

# Best practice for higher availability in librdkafka clients prior to 1.7
session.timeout.ms=45000
```

Start the following command in a separate terminal.

```sh
kcat -b headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443 -C -F ~/tmp/kcat-hq.conf -t headquarter-location-records -f "%k => %s\n"
```

Send events to each incoming topic:

```sh
kcat -b headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443 -P -F ~/tmp/kcat-hq.conf -t warehouse-lis-in -k 55:66:77:88 <<EOF
{"parcelNumber":"55:66:77:88","timestamp":$(date +%s -d "now")}
EOF
kcat -b headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443 -P -F ~/tmp/kcat-hq.conf -t warehouse-lis-out -k 55:66:77:88 <<EOF
{"parcelNumber":"55:66:77:88","timestamp":$(date +%s -d "now")}
EOF
kcat -b headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443 -P -F ~/tmp/kcat-hq.conf -t warehouse-ath-in -k 55:66:77:88 <<EOF
{"parcelNumber":"55:66:77:88","timestamp":$(date +%s -d "now")}
EOF
kcat -b headquarter-kafka-tls-bootstrap-headquarter.apps.appdev.itix.xyz:443 -P -F ~/tmp/kcat-hq.conf -t warehouse-ath-out -k 55:66:77:88 <<EOF
{"parcelNumber":"55:66:77:88","timestamp":$(date +%s -d "now")}
EOF
```

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

Start the following command in a separate terminal.

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
