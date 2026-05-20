package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.KebunDetailResponse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        when(kebunService.findByFilters("Sawit", "")).thenReturn(List.of(kebun1, kebun2));

        mockMvc.perform(get("/kebun").param("name", "Sawit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("KBNA01"))
                .andExpect(jsonPath("$[1].code").value("KBNB02"));
    }

    @Test
    void updateKebunShouldReturnUpdatedPayload() throws Exception {
        Kebun updated = Kebun.builder()
                .name("Kebun Sawit A Updated")
                .code("KBNA01")
                .luas(150.0)
                .build();

        when(kebunService.update(any(), any(Kebun.class))).thenReturn(updated);

        String requestBody = """
                {
                  "name": "Kebun Sawit A Updated",
                  "code": "KBNA01",
                  "luas": 150.0
                }
                """;

        mockMvc.perform(put("/kebun/{code}", "KBNA01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("KBNA01"))
                .andExpect(jsonPath("$.luas").value(150.0));
    }

    @Test
    void deleteKebunShouldReturnNoContent() throws Exception {
        doNothing().when(kebunService).delete("KBNA01");

        mockMvc.perform(delete("/kebun/{code}", "KBNA01"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getKebunListShouldSupportNameAndCodeFilter() throws Exception {
        Kebun kebun = Kebun.builder().name("Kebun Sawit A").code("KBNA01").luas(100.0).build();
        when(kebunService.findByFilters("Sawit", "A01")).thenReturn(List.of(kebun));

        mockMvc.perform(get("/kebun").param("name", "Sawit").param("code", "A01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("KBNA01"));
    }

    @Test
    void getDetailShouldReturnKebunDetail() throws Exception {
        KebunDetailResponse detail = new KebunDetailResponse(
                "KB001",
                "Kebun A",
                100.0,
                List.of(
                        new Kebun.Point(0, 0),
                        new Kebun.Point(0, 2),
                        new Kebun.Point(2, 2),
                        new Kebun.Point(2, 0)
                ),
                "3",
                List.of("11", "12")
        );
        when(kebunService.getKebunDetailByCode("KB001")).thenReturn(detail);

        mockMvc.perform(get("/kebun/{code}/detail", "KB001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("KB001"))
                .andExpect(jsonPath("$.mandorId").value("3"))
                .andExpect(jsonPath("$.supirIds[0]").value("11"));
    }

    @Test
    void assignMandorShouldCallService() throws Exception {
        String body = """
                {
                  "mandorId": "3"
                }
                """;

        mockMvc.perform(post("/kebun/{code}/mandor/assign", "KB001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mandor assigned"));

        verify(kebunService).assignMandor("KB001", "3");
    }

    @Test
    void reassignMandorShouldCallService() throws Exception {
        String body = """
                {
                  "mandorId": "3",
                  "replacementKebunCode": "KB002"
                }
                """;

        mockMvc.perform(post("/kebun/{code}/mandor/reassign", "KB001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mandor reassigned to another kebun"));

        verify(kebunService).reassignMandorToAnotherKebun("KB001", "3", "KB002");
    }

    @Test
    void assignSupirShouldCallService() throws Exception {
        String body = """
                {
                  "supirId": "11"
                }
                """;

        mockMvc.perform(post("/kebun/{code}/supir/assign", "KB001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Supir assigned"));

        verify(kebunService).assignSupir("KB001", "11");
    }

    @Test
    void reassignSupirShouldCallService() throws Exception {
        String body = """
                {
                  "supirId": "11",
                  "replacementKebunCode": "KB002"
                }
                """;

        mockMvc.perform(post("/kebun/{code}/supir/reassign", "KB001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Supir reassigned to another kebun"));

        verify(kebunService).reassignSupirToAnotherKebun("KB001", "11", "KB002");
    }
}
