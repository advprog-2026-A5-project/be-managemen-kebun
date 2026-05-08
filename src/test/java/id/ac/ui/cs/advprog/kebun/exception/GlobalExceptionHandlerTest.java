package id.ac.ui.cs.advprog.kebun.exception;

import id.ac.ui.cs.advprog.kebun.controller.KebunController;
import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KebunController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @Test
    void shouldMapIllegalArgumentExceptionToBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Kebun code is immutable and cannot be changed"))
                .when(kebunService).update(eq("KBNA01"), any(Kebun.class));

        String requestBody = """
                {
                  "name": "Invalid",
                  "code": "DIFFERENT",
                  "luas": 100.0
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "KBNA01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kebun code is immutable and cannot be changed"));
    }

    @Test
    void shouldMapIllegalStateExceptionToConflict() throws Exception {
        doThrow(new IllegalStateException("Cannot delete kebun with active mandor"))
                .when(kebunService).delete("KBNA01");

        mockMvc.perform(delete("/kebun/{code}", "KBNA01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete kebun with active mandor"));
    }
}
