package fr.itix.enricher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class Routes extends RouteBuilder {
    private static final Logger LOG = Logger.getLogger(Routes.class);

    @Override
    public void configure() throws Exception {        
        LOG.infov("Parsing configuration (application.properties / environment variables)");

        HashMap<String, TopicMetadata> topics = new HashMap<String, TopicMetadata>();
        Iterator<String> it = ConfigProvider.getConfig().getPropertyNames().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (!key.startsWith("enricher.topic.")) {
                continue;
            }

            String[] parts = key.split("\\.");
            if (parts.length < 4) {
                continue;
            }

            String[] topicParts = new String[parts.length - 3];
            System.arraycopy(parts, 2, topicParts, 0, parts.length - 3);
            String topic = String.join(".", topicParts);
            LOG.infov("Found topic {0}!", topic);
            topics.put(topic, null);
        }

        it = topics.keySet().iterator();
        while (it.hasNext()) {
            String topic = it.next();
            String warehouse = ConfigProvider.getConfig().getValue("enricher.topic." + topic + ".warehouse", String.class);
            String direction = ConfigProvider.getConfig().getValue("enricher.topic." + topic + ".direction", String.class);
            topics.replace(topic, new TopicMetadata(warehouse, direction));
        }

        String sinkTopic = ConfigProvider.getConfig().getValue("enricher.sink-topic", String.class);

        LOG.infov("Parsing completed!");

        LOG.infov("Generating Camel routes");

        Iterator<Entry<String, TopicMetadata>> it2 = topics.entrySet().iterator();
        while (it2.hasNext()) {
            Entry<String, TopicMetadata> kv = it2.next();
            String topic = kv.getKey();
            TopicMetadata metadata = kv.getValue();
            
            from(String.format("kafka:%s?brokers={{enricher.kafka-brokers}}&securityProtocol={{enricher.kafka.security-protocol}}&saslMechanism={{enricher.kafka.sasl-mechanism}}&saslJaasConfig={{enricher.kafka.jaas-config}}", topic))
                .unmarshal().json(LocationEntity.class)
                .process(new MessageEnricherProcessor(metadata.warehouse, metadata.direction))
                .marshal().json()
                .to(String.format("kafka:%s?brokers={{enricher.kafka-brokers}}&securityProtocol={{enricher.kafka.security-protocol}}&saslMechanism={{enricher.kafka.sasl-mechanism}}&saslJaasConfig={{enricher.kafka.jaas-config}}", sinkTopic));
        }

        LOG.infov("Camel routes generated!");
    }

    private class TopicMetadata {
        String warehouse;
        String direction;

        TopicMetadata(String warehouse, String direction) {
            this.warehouse = warehouse;
            this.direction = direction;
        }
    }
}
