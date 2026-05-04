package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KebunController.class)
class KebunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @Test
    void createKebunShouldReturnCreatedResponse() throws Exception {
        Kebun created = Kebun.builder()
                .name("Kebun Sawit A")
                .code("KBNA01")
                .luas(100.0)
                .build();

        when(kebunService.create(any(Kebun.class))).thenReturn(created);

        String requestBody = """
                {
                  "name": "Kebun Sawit A",
                  "code": "KBNA01",
                  "luas": 100.0
                }
                """;

        mockMvc.perform(post("/kebun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kebun Sawit A"))
                .andExpect(jsonPath("$.code").value("KBNA01"))
                .andExpect(jsonPath("$.luas").value(100.0));
    }
}
