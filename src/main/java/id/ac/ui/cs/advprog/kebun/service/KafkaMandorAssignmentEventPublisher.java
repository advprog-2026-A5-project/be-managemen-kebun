package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMandorAssignmentEventPublisher implements MandorAssignmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMandorAssignmentEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMandorAssignmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String kebunCode, String mandorId) {
        try {
            kafkaTemplate.send(topic, kebunCode, new MandorAssignedEvent(kebunCode, mandorId));
        } catch (Exception ex) {
            log.warn("Skipping mandor assignment event publish because Kafka is unavailable: topic={}, kebunCode={}, mandorId={}", topic, kebunCode, mandorId, ex);
        }
    }
}
