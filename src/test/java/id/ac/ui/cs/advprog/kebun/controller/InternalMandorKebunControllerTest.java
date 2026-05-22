package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.MandorKebunAssignmentResponse;
import id.ac.ui.cs.advprog.kebun.security.InternalApiTokenFilter;
import id.ac.ui.cs.advprog.kebun.security.JwtAuthenticationFilter;
import id.ac.ui.cs.advprog.kebun.service.KebunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalMandorKebunController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalMandorKebunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private InternalApiTokenFilter internalApiTokenFilter;

    @Test
    void getMandorKebunAssignmentShouldReturnActiveWhenAssigned() throws Exception {
        MandorKebunAssignmentResponse response = new MandorKebunAssignmentResponse(3L, null, "KB001", "Kebun A", true);
        when(kebunService.getMandorKebunAssignment(3L)).thenReturn(response);

        mockMvc.perform(get("/internal/mandors/{mandorId}/kebun", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mandorId").value(3))
                .andExpect(jsonPath("$.kebunCode").value("KB001"))
                .andExpect(jsonPath("$.active").value(true));

        verify(kebunService).getMandorKebunAssignment(3L);
    }

    @Test
    void getMandorKebunAssignmentShouldReturnInactiveWhenNotAssigned() throws Exception {
        MandorKebunAssignmentResponse response = new MandorKebunAssignmentResponse(9L, null, null, null, false);
        when(kebunService.getMandorKebunAssignment(9L)).thenReturn(response);

        mockMvc.perform(get("/internal/mandors/{mandorId}/kebun", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mandorId").value(9))
                .andExpect(jsonPath("$.active").value(false));
    }
}
