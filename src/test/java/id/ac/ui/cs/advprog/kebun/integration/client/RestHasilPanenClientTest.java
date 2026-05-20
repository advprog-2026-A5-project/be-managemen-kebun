package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.HasilPanenServiceProperties;
import id.ac.ui.cs.advprog.kebun.integration.dto.MandorAssignedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestHasilPanenClientTest {

    @Test
    void notifyMandorAssignedShouldSendPostRequestToHasilPanenService() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        HasilPanenServiceProperties properties = new HasilPanenServiceProperties();
        properties.setBaseUrl("http://hasil-panen-service");

        RestHasilPanenClient client = new RestHasilPanenClient(builder, properties);
        MandorAssignedRequest request = new MandorAssignedRequest("KB001", "mandor-123");

        server.expect(requestTo("http://hasil-panen-service/api/integration/mandor-assigned"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.kebunCode").value("KB001"))
                .andExpect(jsonPath("$.mandorId").value("mandor-123"))
                .andRespond(withSuccess());

        client.notifyMandorAssigned(request);

        server.verify();
    }
}
