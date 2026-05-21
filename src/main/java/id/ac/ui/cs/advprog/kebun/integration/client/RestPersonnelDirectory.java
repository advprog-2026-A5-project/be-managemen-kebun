package id.ac.ui.cs.advprog.kebun.integration.client;

import id.ac.ui.cs.advprog.kebun.integration.config.AuthServiceProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RestPersonnelDirectory implements PersonnelDirectory {

    private static final String ROLE_MANDOR = "MANDOR";
    private static final String ROLE_SUPIR = "SUPIR";
    private static final String INTERNAL_SERVICE_TOKEN_HEADER = "X-Internal-Service-Token";

    private final AuthServiceProperties authServiceProperties;
    private final RestClient restClient;
    private final Clock clock;
    private final ConcurrentMap<CacheKey, CacheEntry> identityCache = new ConcurrentHashMap<>();

    public RestPersonnelDirectory(RestClient.Builder restClientBuilder,
                                  AuthServiceProperties authServiceProperties) {
        this(restClientBuilder, authServiceProperties, Clock.systemUTC());
    }

    RestPersonnelDirectory(RestClient.Builder restClientBuilder,
                           AuthServiceProperties authServiceProperties,
                           Clock clock) {
        this.authServiceProperties = authServiceProperties;
        this.clock = clock;
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
        InternalUserIdentityResponse identity = resolveIdentity(userId, expectedRole);

        if (identity == null || identity.id() == null || identity.role() == null || identity.role().isBlank()) {
            throw new IllegalStateException("Failed to validate " + expectedRole + " identity");
        }

        if (!expectedRole.equalsIgnoreCase(identity.role())) {
            throw new IllegalArgumentException("User " + userId + " must have role " + expectedRole);
        }

        return String.valueOf(identity.id());
    }

    private InternalUserIdentityResponse resolveIdentity(Long userId, String expectedRole) {
        long ttlSeconds = authServiceProperties.getIdentityCacheTtlSeconds();
        boolean cacheEnabled = ttlSeconds > 0;

        if (cacheEnabled) {
            CacheKey cacheKey = new CacheKey(expectedRole.toUpperCase(), userId);
            CacheEntry cacheEntry = identityCache.get(cacheKey);
            if (cacheEntry != null && !cacheEntry.isExpired(clock.millis())) {
                return cacheEntry.identity();
            }

            InternalUserIdentityResponse fetched = fetchIdentity(userId, expectedRole);
            if (isCacheableSuccess(fetched, expectedRole)) {
                long expiresAtMillis = clock.millis() + (ttlSeconds * 1000L);
                identityCache.put(cacheKey, new CacheEntry(fetched, expiresAtMillis));
            }
            return fetched;
        }

        return fetchIdentity(userId, expectedRole);
    }

    private boolean isCacheableSuccess(InternalUserIdentityResponse identity, String expectedRole) {
        return identity != null
                && identity.id() != null
                && identity.role() != null
                && !identity.role().isBlank()
                && expectedRole.equalsIgnoreCase(identity.role());
    }

    private Long parseUserId(String rawId, String expectedRole) {
        try {
            if (rawId == null || rawId.isBlank()) {
                throw new NumberFormatException("blank id");
            }
            return Long.valueOf(rawId.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(expectedRole + " ID must be a valid numeric user ID", ex);
        }
    }

    private InternalUserIdentityResponse fetchIdentity(Long userId, String expectedRole) {
        try {
            return restClient.get()
                    .uri("/internal/users/{id}/identity", userId)
                    .header(INTERNAL_SERVICE_TOKEN_HEADER, authServiceProperties.getInternalServiceToken())
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

    private record CacheKey(String expectedRole, Long userId) {
        CacheKey {
            Objects.requireNonNull(expectedRole);
            Objects.requireNonNull(userId);
        }
    }

    private record CacheEntry(InternalUserIdentityResponse identity, long expiresAtMillis) {
        private boolean isExpired(long nowMillis) {
            return nowMillis >= expiresAtMillis;
        }
    }
}
