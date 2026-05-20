package id.ac.ui.cs.advprog.kebun.service;

public interface MandorAssignmentEventPublisher {
    void publish(String topic, String kebunCode, String mandorId);
}
