package id.ac.ui.cs.advprog.kebun.service;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMandorAssignmentEventPublisher implements MandorAssignmentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMandorAssignmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, String kebunCode, String mandorId) {
        kafkaTemplate.send(topic, kebunCode, new MandorAssignedEvent(kebunCode, mandorId));
    }
}
