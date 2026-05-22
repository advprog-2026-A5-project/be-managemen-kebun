package id.ac.ui.cs.advprog.kebun.security;

import id.ac.ui.cs.advprog.kebun.controller.InternalMandorKebunController;
import id.ac.ui.cs.advprog.kebun.controller.KebunController;
import id.ac.ui.cs.advprog.kebun.dto.MandorKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({KebunController.class, InternalMandorKebunController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        InternalApiTokenFilter.class,
        JwtTokenService.class,
        SecurityProperties.class
})
@TestPropertySource(properties = {
        "mysawit.security.jwt-secret=test-secret-value-that-is-at-least-sixty-four-characters-long-123456",
        "mysawit.security.internal-api-token=test-internal-token"
})
class SecurityIntegrationTest {

    private static final String JWT_SECRET = "test-secret-value-that-is-at-least-sixty-four-characters-long-123456";
    private static final String INTERNAL_TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @Test
    void kebunEndpointShouldRejectWhenJwtMissing() throws Exception {
        mockMvc.perform(get("/kebun"))
                .andExpect(result -> assertUnauthorizedOrForbidden(result.getResponse().getStatus()));
    }

    @Test
    void kebunEndpointShouldRejectWhenJwtInvalid() throws Exception {
        mockMvc.perform(get("/kebun").header("Authorization", "Bearer invalid-token"))
                .andExpect(result -> assertUnauthorizedOrForbidden(result.getResponse().getStatus()));
    }

    @Test
    void kebunEndpointShouldAllowWhenJwtValid() throws Exception {
        when(kebunService.findByFilters("", "")).thenReturn(List.of(
                Kebun.builder().code("KB001").name("Kebun A").luas(100.0).coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                )).build()
        ));

        mockMvc.perform(get("/kebun").header("Authorization", "Bearer " + createValidToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("KB001"));
    }

    @Test
    void internalEndpointShouldRejectWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/internal/mandors/{mandorId}/kebun", 7L))
                .andExpect(result -> assertUnauthorizedOrForbidden(result.getResponse().getStatus()));
    }

    @Test
    void internalEndpointShouldRejectWhenTokenInvalid() throws Exception {
        mockMvc.perform(get("/internal/mandors/{mandorId}/kebun", 7L)
                        .header("X-Internal-Api-Token", "wrong-token"))
                .andExpect(result -> assertUnauthorizedOrForbidden(result.getResponse().getStatus()));
    }

    @Test
    void internalEndpointShouldAllowWhenTokenValid() throws Exception {
        when(kebunService.getMandorKebunAssignment(7L)).thenReturn(
                new MandorKebunAssignmentResponse(7L, null, "KB001", "Kebun A", true)
        );

        mockMvc.perform(get("/internal/mandors/{mandorId}/kebun", 7L)
                        .header("X-Internal-Api-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kebunCode").value("KB001"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void kebunCreateShouldAllowWhenJwtValid() throws Exception {
        Kebun created = Kebun.builder()
                .code("KB101")
                .name("Kebun Create")
                .luas(10.0)
                .coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                ))
                .build();
        when(kebunService.create(any(Kebun.class))).thenReturn(created);

        String body = """
                {
                  "code": "KB101",
                  "name": "Kebun Create",
                  "luas": 10.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(post("/kebun")
                        .header("Authorization", "Bearer " + createValidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KB101"));
    }

    @Test
    void kebunUpdateShouldAllowWhenJwtValid() throws Exception {
        Kebun updated = Kebun.builder()
                .code("KB101")
                .name("Kebun Updated")
                .luas(12.0)
                .coordinates(List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 1),
                        new Kebun.Point(1, 1),
                        new Kebun.Point(1, 0)
                ))
                .build();
        when(kebunService.update(eq("KB101"), any(Kebun.class))).thenReturn(updated);

        String body = """
                {
                  "code": "KB101",
                  "name": "Kebun Updated",
                  "luas": 12.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "KB101")
                        .header("Authorization", "Bearer " + createValidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kebun Updated"));
    }

    @Test
    void kebunDeleteShouldAllowWhenJwtValid() throws Exception {
        mockMvc.perform(delete("/kebun/{code}", "KB101")
                        .header("Authorization", "Bearer " + createValidToken()))
                .andExpect(status().isNoContent());
    }

    private String createValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject("admin@mysawit.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private void assertUnauthorizedOrForbidden(int status) {
        assertTrue(status == 401 || status == 403);
    }
}
