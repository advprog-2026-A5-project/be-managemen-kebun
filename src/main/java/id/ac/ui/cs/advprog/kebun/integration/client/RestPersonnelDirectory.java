package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.AuthServiceProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestPersonnelDirectory implements PersonnelDirectory {

    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";

    private final RestClient restClient;

    public RestPersonnelDirectory(RestClient.Builder restClientBuilder,
                                  AuthServiceProperties authServiceProperties) {
        this.restClient = restClientBuilder
                .baseUrl(authServiceProperties.getBaseUrl())
                .build();
    }

    @Override
    public String requireMandorId(String rawId) {
        return requireUserWithRole(rawId, ROLE_MANDOR);
    }

    @Override
    public String requireSupirId(String rawId) {
        return requireUserWithRole(rawId, ROLE_SUPIR);
    }

    private String requireUserWithRole(String rawId, String expectedRole) {
        Long userId = parseUserId(rawId, expectedRole);
        InternalUserIdentityResponse identity = fetchIdentity(userId, expectedRole);
        if (identity == null || identity.id() == null || identity.role() == null || identity.role().isBlank()) {
            throw new IllegalStateException("Failed to validate " + expectedRole + " identity");
        }
        if (!expectedRole.equalsIgnoreCase(identity.role())) {
            throw new IllegalArgumentException("User " + userId + " must have role " + expectedRole);
        }
        return String.valueOf(identity.id());
    }

    private Long parseUserId(String rawId, String expectedRole) {
        try {
            return Long.valueOf(rawId.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(expectedRole + " ID must be a valid numeric user ID", ex);
        }
    }

    private InternalUserIdentityResponse fetchIdentity(Long userId, String expectedRole) {
        try {
            return restClient.get()
                    .uri("/internal/users/{id}/identity", userId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new IllegalArgumentException(expectedRole + " user not found: " + userId);
                    })
                    .body(InternalUserIdentityResponse.class);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException(expectedRole + " user not found: " + userId, ex);
            }
            throw new IllegalStateException("Failed to validate " + expectedRole + " identity", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to validate " + expectedRole + " identity", ex);
        }
    }

    private record InternalUserIdentityResponse(Long id, String email, String nama, String role) {
    }
}
