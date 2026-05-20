package id.ac.ui.cs.advprog.kebun.integration.facade;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.client.HasilPanenClient;
import id.ac.ui.cs.advprog.kebun.integration.dto.MandorAssignedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class MandorIntegrationFacade {

    private static final Logger log = LoggerFactory.getLogger(MandorIntegrationFacade.class);

    private final HasilPanenClient hasilPanenClient;

    public MandorIntegrationFacade(HasilPanenClient hasilPanenClient) {
        this.hasilPanenClient = hasilPanenClient;
    }

    public void notifyMandorAssigned(MandorAssignedEvent event) {
        MandorAssignedRequest request = MandorAssignedRequest.fromEvent(event);
        try {
            hasilPanenClient.notifyMandorAssigned(request);
        } catch (RestClientException ex) {
            log.warn(
                    "Failed to notify Hasil Panen service about mandor assignment: kebunCode={}, mandorId={}, reason={}",
                    event.getKebunCode(),
                    event.getMandorId(),
                    ex.getMessage()
            );
        }
    }
}
