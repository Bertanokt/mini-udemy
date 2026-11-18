package com.miniudemy.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.miniudemy.auth.domain.Role;
import com.miniudemy.auth.domain.User;
import com.miniudemy.auth.repository.UserRepository;
import com.miniudemy.auth.service.AccountLookupService;
import com.miniudemy.common.security.JwtConfig;
import com.miniudemy.common.security.JwtService;
import com.miniudemy.common.security.SecurityConfig;
import com.miniudemy.course.domain.CourseStateException;
import com.miniudemy.course.dto.SectionRequest;
import com.miniudemy.course.dto.SectionResponse;
import com.miniudemy.course.service.SectionService;

/** Bölüm uçları (#24) gerçek güvenlik ayarıyla; iş kuralları SectionServiceTest'te. */
@WebMvcTest(controllers = SectionController.class)
@Import({SecurityConfig.class, JwtConfig.class, AccountLookupService.class})
@ActiveProfiles("test")
class SectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SectionService sectionService;

    @BeforeEach
    void users() {
        user(7L, "Mehmet Kaya", Role.INSTRUCTOR);
        user(42L, "Ayşe Yılmaz", Role.STUDENT);
        user(1L, "Ali Yönetici", Role.ADMIN);
    }

    private static SectionResponse section(long id, String title, int position) {
        return new SectionResponse(id, 5L, title, position, List.of());
    }

    // --- Listeleme (herkese açık) ---

    @Test
    void list_withoutToken_isPublic_andPassesNoViewer() throws Exception {
        given(sectionService.listSections(5L, null, false))
                .willReturn(List.of(section(11L, "Giriş", 0), section(12L, "Nesneler", 1)));

        mockMvc.perform(get("/api/courses/5/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Giriş"))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    @Test
    void list_withToken_passesViewerIdAndAdminFlag() throws Exception {
        given(sectionService.listSections(5L, 7L, false)).willReturn(List.of(section(11L, "Giriş", 0)));
        given(sectionService.listSections(5L, 1L, true)).willReturn(List.of(section(11L, "Giriş", 0)));

        mockMvc.perform(get("/api/courses/5/sections").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
        mockMvc.perform(get("/api/courses/5/sections").header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    // --- Ekleme ---

    @Test
    void create_asInstructor_returns201_forTokenOwner() throws Exception {
        given(sectionService.createSection(5L, 7L, new SectionRequest("Akışlar")))
                .willReturn(section(14L, "Akışlar", 3));

        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"Akışlar\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(14))
                .andExpect(jsonPath("$.courseId").value(5))
                .andExpect(jsonPath("$.position").value(3));
    }

    @Test
    void create_withBlankOrTooLongTitle_returns400OnTitleField() throws Exception {
        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\" \"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Bölüm başlığı zorunludur."));
        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"" + "a".repeat(201) + "\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").value("Bölüm başlığı en fazla 200 karakter olabilir."));
        verify(sectionService, never()).createSection(any(), any(), any());
    }

    @Test
    void create_withoutToken_returns401_asStudentOrAdmin_returns403() throws Exception {
        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"Akışlar\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"Akışlar\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"Akışlar\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1L, "ADMIN")))
                .andExpect(status().isForbidden());
        verify(sectionService, never()).createSection(any(), any(), any());
    }

    @Test
    void create_whenCourseIsLocked_returns409WithStandardBody() throws Exception {
        given(sectionService.createSection(5L, 7L, new SectionRequest("Akışlar")))
                .willThrow(new CourseStateException("Kurs bu durumdayken düzenlenemez."));

        mockMvc.perform(json(post("/api/courses/5/sections"), "{\"title\":\"Akışlar\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Kurs bu durumdayken düzenlenemez."));
    }

    // --- Yeniden adlandırma, silme, taşıma ---

    @Test
    void update_asInstructor_returns200() throws Exception {
        given(sectionService.updateSection(12L, 7L, new SectionRequest("Sınıflar")))
                .willReturn(section(12L, "Sınıflar", 1));

        mockMvc.perform(json(put("/api/sections/12"), "{\"title\":\"Sınıflar\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Sınıflar"));
    }

    @Test
    void delete_asInstructor_returns204_andCallsServiceWithTokenOwner() throws Exception {
        mockMvc.perform(delete("/api/sections/12").header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isNoContent());
        verify(sectionService).deleteSection(12L, 7L);
    }

    @Test
    void delete_asStudent_returns403_andDeletesNothing() throws Exception {
        mockMvc.perform(delete("/api/sections/12").header(HttpHeaders.AUTHORIZATION, bearer(42L, "STUDENT")))
                .andExpect(status().isForbidden());
        verify(sectionService, never()).deleteSection(any(), any());
    }

    @Test
    void move_asInstructor_returns200WithNewOrder() throws Exception {
        given(sectionService.moveSection(11L, 7L, 1))
                .willReturn(List.of(section(12L, "Nesneler", 0), section(11L, "Giriş", 1)));

        mockMvc.perform(json(put("/api/sections/11/position"), "{\"position\":1}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[1].id").value(11));
    }

    @Test
    void move_withNegativeOrMissingPosition_returns400OnPositionField() throws Exception {
        mockMvc.perform(json(put("/api/sections/11/position"), "{\"position\":-1}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.position").value("Sıra 0 ya da daha büyük olmalıdır."));
        mockMvc.perform(json(put("/api/sections/11/position"), "{}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(7L, "INSTRUCTOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.position").value("Sıra zorunludur."));
        verify(sectionService, never()).moveSection(any(), any(), anyInt());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void user(long id, String name, Role role) {
        User user = new User(name.toLowerCase().replace(' ', '.') + "@example.com", "$2a$10$ozet", name, role);
        ReflectionTestUtils.setField(user, "id", id);
        given(userRepository.findById(id)).willReturn(Optional.of(user));
    }

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.generateToken(userId, role);
    }
}
