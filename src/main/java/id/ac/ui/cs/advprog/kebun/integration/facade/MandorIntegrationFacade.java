package id.ac.ui.cs.advprog.kebun.integration.facade;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import org.springframework.stereotype.Service;

@Service
public class MandorIntegrationFacade {

    public void notifyMandorAssigned(MandorAssignedEvent event) {
        // Integration implementation will be provided by dedicated client adapter.
    }
}
