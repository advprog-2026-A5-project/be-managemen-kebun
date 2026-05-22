package id.ac.ui.cs.advprog.kebun.controller;

import id.ac.ui.cs.advprog.kebun.dto.SupirKebunAssignmentResponse;
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

@WebMvcTest(InternalSupirKebunController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalSupirKebunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KebunService kebunService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private InternalApiTokenFilter internalApiTokenFilter;

    @Test
    void getSupirKebunAssignmentShouldReturnActiveWhenAssigned() throws Exception {
        SupirKebunAssignmentResponse response = new SupirKebunAssignmentResponse(11L, null, "KB001", "Kebun A", true);
        when(kebunService.getSupirKebunAssignment(11L)).thenReturn(response);

        mockMvc.perform(get("/internal/supirs/{supirId}/kebun", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supirId").value(11))
                .andExpect(jsonPath("$.kebunCode").value("KB001"))
                .andExpect(jsonPath("$.active").value(true));

        verify(kebunService).getSupirKebunAssignment(11L);
    }

    @Test
    void getSupirKebunAssignmentShouldReturnInactiveWhenNotAssigned() throws Exception {
        SupirKebunAssignmentResponse response = new SupirKebunAssignmentResponse(12L, null, null, null, false);
        when(kebunService.getSupirKebunAssignment(12L)).thenReturn(response);

        mockMvc.perform(get("/internal/supirs/{supirId}/kebun", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supirId").value(12))
                .andExpect(jsonPath("$.active").value(false));
    }
}
