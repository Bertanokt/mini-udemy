package com.miniudemy.common.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miniudemy.common.security.AccountLookup;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.SecurityConfig;

/** Gerçek güvenlik ayarıyla: /api/health token olmadan açık olmalı (docker-compose ve frontend buna bakar). */
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@ActiveProfiles("test")
class HealthControllerTest {

    @MockitoBean
    private AccountLookup accountLookup;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReturnsUp_withoutToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("mini-udemy-backend"));
    }
}
