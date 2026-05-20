package id.ac.ui.cs.advprog.kebun.listener;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.facade.MandorIntegrationFacade;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MandorAssignedEventListener {

    private final MandorIntegrationFacade mandorIntegrationFacade;

    public MandorAssignedEventListener(MandorIntegrationFacade mandorIntegrationFacade) {
        this.mandorIntegrationFacade = mandorIntegrationFacade;
    }

    @EventListener
    public void handle(MandorAssignedEvent event) {
        mandorIntegrationFacade.notifyMandorAssigned(event);
    }
}
