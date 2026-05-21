package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.AuthServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
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
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/11/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
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
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/99/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withStatus(NOT_FOUND));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directory.requireMandorId("99"));

        assertEquals("MANDOR user not found: 99", ex.getMessage());
    }

    @Test
    void requireMandorIdShouldRejectNonNumericUserId() {
        RestClient.Builder builder = RestClient.builder();
        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directory.requireMandorId("mandor-abc"));

        assertEquals("MANDOR ID must be a valid numeric user ID", ex.getMessage());
    }

    @Test
    void requireSupirIdShouldRejectNonNumericUserId() {
        RestClient.Builder builder = RestClient.builder();
        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> directory.requireSupirId("supir-xyz"));

        assertEquals("SUPIR ID must be a valid numeric user ID", ex.getMessage());
    }

    @Test
    void requireSupirIdShouldReturnCanonicalNumericIdentifierWhenUserRoleMatches() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/5/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withSuccess("""
                        {"id":5,"email":"supir@mysawit.id","nama":"Supir","role":"SUPIR"}
                        """, MediaType.APPLICATION_JSON));

        String canonicalId = directory.requireSupirId("5");

        assertEquals("5", canonicalId);
        server.verify();
    }

    @Test
    void requireMandorIdShouldThrowIllegalStateWhenAuthServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> directory.requireMandorId("7"));

        assertEquals("Failed to validate MANDOR identity", ex.getMessage());
    }

    @Test
    void requireMandorIdShouldThrowIllegalStateWhenIdentityPayloadIsIncomplete() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");

        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties);

        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withSuccess("""
                        {"id":7,"email":"mandor@mysawit.id","nama":"Mandor"}
                        """, MediaType.APPLICATION_JSON));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> directory.requireMandorId("7"));

        assertEquals("Failed to validate MANDOR identity", ex.getMessage());
    }

    @Test
    void requireMandorIdShouldUseCacheForRepeatedSuccessWithinTtl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(30);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/3/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withSuccess("""
                        {"id":3,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("3", directory.requireMandorId("3"));
        assertEquals("3", directory.requireMandorId("3"));
        server.verify();
    }

    @Test
    void requireSupirIdShouldUseCacheForRepeatedSuccessWithinTtl() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(30);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/5/identity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "token-123"))
                .andRespond(withSuccess("""
                        {"id":5,"email":"supir@mysawit.id","nama":"Supir","role":"SUPIR"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("5", directory.requireSupirId("5"));
        assertEquals("5", directory.requireSupirId("5"));
        server.verify();
    }

    @Test
    void requireSupirIdShouldNotCacheWrongRoleResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(30);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/11/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":11,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth-service/internal/users/11/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":11,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(IllegalArgumentException.class, () -> directory.requireSupirId("11"));
        assertThrows(IllegalArgumentException.class, () -> directory.requireSupirId("11"));
        server.verify();
    }

    @Test
    void requireMandorIdShouldRefetchWhenCacheEntryExpired() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(1);
        MutableClock clock = new MutableClock();
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, clock);

        server.expect(requestTo("http://auth-service/internal/users/3/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":3,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth-service/internal/users/3/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":3,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("3", directory.requireMandorId("3"));
        clock.plusSeconds(2);
        assertEquals("3", directory.requireMandorId("3"));
        server.verify();
    }

    @Test
    void requireMandorIdShouldNotCacheFailed4xxLookup() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(30);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/99/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(NOT_FOUND));
        server.expect(requestTo("http://auth-service/internal/users/99/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(NOT_FOUND));

        assertThrows(IllegalArgumentException.class, () -> directory.requireMandorId("99"));
        assertThrows(IllegalArgumentException.class, () -> directory.requireMandorId("99"));
        server.verify();
    }

    @Test
    void requireMandorIdShouldNotCacheUpstreamFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(30);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(BAD_GATEWAY));
        server.expect(requestTo("http://auth-service/internal/users/7/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(BAD_GATEWAY));

        assertThrows(IllegalStateException.class, () -> directory.requireMandorId("7"));
        assertThrows(IllegalStateException.class, () -> directory.requireMandorId("7"));
        server.verify();
    }

    @Test
    void requireMandorIdShouldDisableCacheWhenTtlIsZeroOrNegative() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        AuthServiceProperties properties = newPropertiesWithTtl(0);
        RestPersonnelDirectory directory = new RestPersonnelDirectory(builder, properties, new MutableClock());

        server.expect(requestTo("http://auth-service/internal/users/3/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":3,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://auth-service/internal/users/3/identity"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":3,"email":"mandor@mysawit.id","nama":"Mandor","role":"MANDOR"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("3", directory.requireMandorId("3"));
        assertEquals("3", directory.requireMandorId("3"));
        server.verify();
    }

    private AuthServiceProperties newPropertiesWithTtl(long ttlSeconds) {
        AuthServiceProperties properties = new AuthServiceProperties();
        properties.setBaseUrl("http://auth-service");
        properties.setInternalServiceToken("token-123");
        properties.setIdentityCacheTtlSeconds(ttlSeconds);
        return properties;
    }

    private static final class MutableClock extends Clock {
        private Instant current = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void plusSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }
    }
}
