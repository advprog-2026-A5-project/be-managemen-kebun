package id.ac.ui.cs.advprog.kebun.integration.facade;

import id.ac.ui.cs.advprog.kebun.event.MandorAssignedEvent;
import id.ac.ui.cs.advprog.kebun.integration.client.HasilPanenClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MandorIntegrationFacadeTest {

    @Mock
    private HasilPanenClient hasilPanenClient;

    @Test
    void notifyMandorAssignedShouldMapEventAndDelegateToClient() {
        MandorIntegrationFacade facade = new MandorIntegrationFacade(hasilPanenClient);
        MandorAssignedEvent event = new MandorAssignedEvent("KB001", "mandor-123");

        facade.notifyMandorAssigned(event);

        verify(hasilPanenClient).notifyMandorAssigned(argThat(request ->
                "KB001".equals(request.kebunCode()) && "mandor-123".equals(request.mandorId())
        ));
    }

    @Test
    void notifyMandorAssignedShouldNotThrowWhenClientFails() {
        MandorIntegrationFacade facade = new MandorIntegrationFacade(hasilPanenClient);
        MandorAssignedEvent event = new MandorAssignedEvent("KB001", "mandor-123");
        doThrow(new RestClientException("downstream error"))
                .when(hasilPanenClient)
                .notifyMandorAssigned(any());

        assertDoesNotThrow(() -> facade.notifyMandorAssigned(event));
    }
}
