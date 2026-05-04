package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.model.Kebun;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getKebunByCodeShouldReturnKebunWhenFound() throws Exception {
        Kebun kebun = Kebun.builder()
                .name("Kebun Sawit B")
                .code("KBNB02")
                .luas(120.0)
                .build();

        when(kebunService.getByCode("KBNB02")).thenReturn(Optional.of(kebun));

        mockMvc.perform(get("/kebun/{code}", "KBNB02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("KBNB02"))
                .andExpect(jsonPath("$.name").value("Kebun Sawit B"));
    }

    @Test
    void getKebunByCodeShouldReturnNotFoundWhenMissing() throws Exception {
        when(kebunService.getByCode("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/kebun/{code}", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getKebunListShouldSupportNameFilter() throws Exception {
        Kebun kebun1 = Kebun.builder().name("Kebun Sawit A").code("KBNA01").luas(100.0).build();
        Kebun kebun2 = Kebun.builder().name("Kebun Sawit B").code("KBNB02").luas(200.0).build();

        when(kebunService.findByName("Sawit")).thenReturn(List.of(kebun1, kebun2));

        mockMvc.perform(get("/kebun").param("name", "Sawit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("KBNA01"))
                .andExpect(jsonPath("$[1].code").value("KBNB02"));
    }
}
