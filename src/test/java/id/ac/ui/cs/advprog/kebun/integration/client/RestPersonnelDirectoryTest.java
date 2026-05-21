package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.AuthServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class RestPersonnelDirectoryTest {

    @Test
    void requireMandorIdShouldReturnCanonicalNumericIdentifierWhenUserRoleMatches() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":7,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        String canonicalId = directory.requireMandorId("7");

        assertEquals("7", canonicalId);
        server.verify();
    }

    @Test
    void requireSupirIdShouldRejectUsersWithWrongRole() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/11/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":11,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directory.requireSupirId("11"));

        assertEquals("User 11 must have role SUPIR", ex.getMessage());
    }

    @Test
    void requireMandorIdShouldRejectUnknownUsers() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/99/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(NOT_FOUND));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directory.requireMandorId("99"));

        assertEquals("MANDOR user not found: 99", ex.getMessage());
    }
}
