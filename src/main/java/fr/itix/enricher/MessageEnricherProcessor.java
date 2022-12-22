package fr.itix.enricher;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class MessageEnricherProcessor implements Processor {
    private String warehouse;
    private String direction;

    public MessageEnricherProcessor(String warehouse, String direction) {
        this.warehouse = warehouse;
        this.direction = direction;
    }

    public void process(Exchange exchange) throws Exception {
        Message msg = exchange.getIn();
        LocationEntity body = msg.getBody(LocationEntity.class);
        body.direction = this.direction;
        body.location = this.warehouse;
        msg.setBody(body, LocationEntity.class);
    }
}
