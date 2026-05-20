package id.ac.ui.cs.advprog.kebun.listener;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.facade.MandorIntegrationFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MandorAssignedEventListenerTest {

    @Mock
    private MandorIntegrationFacade mandorIntegrationFacade;

    @InjectMocks
    private MandorAssignedEventListener mandorAssignedEventListener;

    @Test
    void handleShouldDelegateToMandorIntegrationFacade() {
        MandorAssignedEvent event = new MandorAssignedEvent("KB001", "mandor-123");

        mandorAssignedEventListener.handle(event);

        verify(mandorIntegrationFacade).notifyMandorAssigned(event);
    }
}
