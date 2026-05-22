package id.ac.ui.cs.advprog.kebun.exception;

import id.ac.ui.cs.advprog.kebun.controller.KebunController;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.security.InternalApiTokenFilter;
import id.ac.ui.cs.advprog.kebun.security.JwtAuthenticationFilter;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KebunController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private InternalApiTokenFilter internalApiTokenFilter;

    @Test
    void shouldMapIllegalArgumentExceptionToBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Kebun code is immutable and cannot be changed"))
                .when(kebunService).update(eq("KBNA01"), any(Kebun.class));

        String requestBody = """
                {
                  "name": "Invalid",
                  "code": "DIFFERENT",
                  "luas": 100.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "KBNA01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Kebun code is immutable and cannot be changed"));
    }

    @Test
    void shouldMapIllegalStateExceptionToConflict() throws Exception {
        doThrow(new IllegalStateException("Cannot delete kebun with active mandor"))
                .when(kebunService).delete("KBNA01");

        mockMvc.perform(delete("/kebun/{code}", "KBNA01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete kebun with active mandor"));
    }

    @Test
    void shouldMapNoSuchElementExceptionToNotFound() throws Exception {
        doThrow(new NoSuchElementException("Kebun not found with code: UNKNOWN"))
                .when(kebunService).update(eq("UNKNOWN"), any(Kebun.class));

        String requestBody = """
                {
                  "name": "Missing",
                  "code": "UNKNOWN",
                  "luas": 100.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Kebun not found with code: UNKNOWN"));
    }

    @Test
    void shouldMapDuplicateCreateToConflict() throws Exception {
        doThrow(new IllegalStateException("Kebun with code already exists: KBNA01"))
                .when(kebunService).create(any(Kebun.class));

        String requestBody = """
                {
                  "name": "Duplicate",
                  "code": "KBNA01",
                  "luas": 100.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(post("/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Kebun with code already exists: KBNA01"));
    }

    @Test
    void shouldMapOverlapToConflict() throws Exception {
        doThrow(new IllegalStateException("Kebun coordinates overlap with an existing kebun"))
                .when(kebunService).update(eq("KBNA01"), any(Kebun.class));

        String requestBody = """
                {
                  "name": "Overlap",
                  "code": "KBNA01",
                  "luas": 100.0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 },
                    { "x": 1, "y": 0 }
                  ]
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "KBNA01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("409"))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Kebun coordinates overlap with an existing kebun"));
    }

    @Test
    void shouldMapInvalidCreatePayloadToBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": " ",
                  "code": "KBNA01",
                  "luas": 0,
                  "coordinates": [
                    { "x": 0, "y": 0 },
                    { "x": 0, "y": 1 },
                    { "x": 1, "y": 1 }
                  ]
                }
                """;

        mockMvc.perform(post("/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("400"))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldMapMissingDeleteToNotFound() throws Exception {
        doThrow(new NoSuchElementException("Kebun not found with code: UNKNOWN"))
                .when(kebunService).delete("UNKNOWN");

        mockMvc.perform(delete("/kebun/{code}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Kebun not found with code: UNKNOWN"));
    }

    @Test
    void shouldMapUnexpectedErrorsToGenericInternalServerError() throws Exception {
        doThrow(new RuntimeException("database down"))
                .when(kebunService).getKebunDetailByCode("KBNA01");

        mockMvc.perform(get("/kebun/{code}/detail", "KBNA01"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("500"))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
