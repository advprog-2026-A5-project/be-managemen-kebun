package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.HasilPanenServiceProperties;
import id.ac.ui.cs.advprog.kebun.integration.dto.MandorAssignedRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestHasilPanenClient implements HasilPanenClient {

    private static final String MANDOR_ASSIGNED_ENDPOINT = "/api/integration/mandor-assigned";

    private final RestClient restClient;

    public RestHasilPanenClient(RestClient.Builder restClientBuilder,
                                HasilPanenServiceProperties hasilPanenServiceProperties) {
        this.restClient = restClientBuilder
                .baseUrl(hasilPanenServiceProperties.getBaseUrl())
                .build();
    }

    @Override
    public void notifyMandorAssigned(MandorAssignedRequest request) {
        restClient.post()
                .uri(MANDOR_ASSIGNED_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
