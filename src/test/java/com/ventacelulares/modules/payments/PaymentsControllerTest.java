package com.ventacelulares.modules.payments;

import com.ventacelulares.modules.payments.api.PaymentsController;
import com.ventacelulares.modules.payments.application.PaymentsService;
import com.ventacelulares.shared.exception.NotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentsController.class)
class PaymentsControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaymentsService service;

    @Test
    @WithMockUser(roles = "CLIENTE")
    void rejectsMissingConsultationDate() throws Exception {
        mvc.perform(get("/api/v1/payments/variants/samsung-galaxy-a55-256-black/options")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VENDEDOR")
    void rejectsInvalidConsultationDate() throws Exception {
        mvc.perform(get("/api/v1/payments/variants/samsung-galaxy-a55-256-black/options")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unknownVariantHasNoPaymentOptions() throws Exception {
        when(service.getPaymentOptions("missing", LocalDate.of(2026, 5, 18)))
                .thenThrow(new NotFoundException("No se pudo identificar la variante elegida"));
        mvc.perform(get("/api/v1/payments/variants/missing/options")
                        .param("date", "2026-05-18"))
                .andExpect(status().isNotFound());
    }
}
