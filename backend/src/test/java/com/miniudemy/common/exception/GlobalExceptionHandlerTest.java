package com.miniudemy.common.exception;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * GlobalExceptionHandler'ın her hata türünü sözleşmedeki ErrorResponse gövdesine çevirdiğini doğrular.
 *
 * Henüz gerçek API ucu olmadığı için istisna fırlatan uçlar aşağıdaki iç sınıfta (TestEndpoints).
 * Spring Boot test sınıflarının iç sınıflarını bileşen taramasından hariç tuttuğu için bu uçlar
 * uygulamaya karışmaz; teste @Import ile eklenir.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestEndpoints.class)
@Import(GlobalExceptionHandlerTest.TestEndpoints.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    private static final String SECRET = "SELECT * FROM users -- iç ayrıntı";

    @Autowired
    private MockMvc mockMvc;

    // ---------------------------------------------------------------- kabul kriteri 1: doğrulama

    @Test
    void invalidBody_returns400WithFieldErrors() throws Exception {
        String body = """
                {"name": "", "email": "gecersiz", "password": "123"}
                """;
        mockMvc.perform(post("/test-errors/validation").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.VALIDATION_MESSAGE))
                .andExpect(jsonPath("$.errors", aMapWithSize(3)))
                .andExpect(jsonPath("$.errors.name").value("Ad zorunludur."))
                .andExpect(jsonPath("$.errors.email").value("Geçerli bir e-posta adresi girin."))
                .andExpect(jsonPath("$.errors.password").value("Parola en az 8 karakter olmalıdır."));
    }

    @Test
    void validBody_passesThrough() throws Exception {
        String body = """
                {"name": "Ada", "email": "ada@example.com", "password": "12345678"}
                """;
        mockMvc.perform(post("/test-errors/validation").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- kabul kriteri 2: kendi istisnalarımız

    @Test
    void notFoundException_returns404() throws Exception {
        expectError(get("/test-errors/not-found"), 404, "Kurs bulunamadı.");
    }

    @Test
    void forbiddenException_returns403() throws Exception {
        expectError(get("/test-errors/forbidden"), 403, "Bu kursu düzenleme yetkiniz yok.");
    }

    @Test
    void conflictException_returns409() throws Exception {
        expectError(get("/test-errors/conflict"), 409, "Bu kursa zaten kayıtlısınız.");
    }

    @Test
    void conflictWithFieldError_returnsContractExample() throws Exception {
        // docs/api/openapi.yaml › responses › Conflict örneğinin aynısı
        mockMvc.perform(get("/test-errors/conflict-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Bu e-posta adresi zaten kayıtlı."))
                .andExpect(jsonPath("$.errors", aMapWithSize(1)))
                .andExpect(jsonPath("$.errors.email").value("Bu e-posta adresi zaten kayıtlı."));
    }

    @Test
    void badRequestException_returns400WithEmptyErrors() throws Exception {
        expectError(get("/test-errors/bad-request"), 400, "Her soruda en az bir doğru şık olmalıdır.");
    }

    @Test
    void payloadTooLargeException_returns413() throws Exception {
        expectError(get("/test-errors/too-large"), 413, "Video en fazla 200 MB olabilir.");
    }

    @Test
    void unsupportedMediaTypeException_returns415() throws Exception {
        expectError(get("/test-errors/unsupported-type"), 415, "Yalnız mp4 ve webm videolar yüklenebilir.");
    }

    // ---------------------------------------------------------------- kabul kriteri 3: beklenmeyen hata

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        expectError(get("/test-errors/boom"), 500, GlobalExceptionHandler.UNEXPECTED_MESSAGE)
                .andExpect(content().string(not(containsString("SELECT"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at com.miniudemy"))));
    }

    // ---------------------------------------------------------------- Spring'in kendi hataları da aynı gövdeyle

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/test-errors/validation").contentType(MediaType.APPLICATION_JSON).content("{bozuk"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("İstek gövdesi okunamadı; JSON biçimini kontrol edin."))
                .andExpect(jsonPath("$.errors", aMapWithSize(0)));
    }

    @Test
    void wrongParameterType_returns400WithParameterError() throws Exception {
        mockMvc.perform(get("/test-errors/typed").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").value("Geçersiz değer."));
    }

    @Test
    void missingParameter_returns400WithParameterError() throws Exception {
        mockMvc.perform(get("/test-errors/typed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.page").value("Zorunlu alan."));
    }

    @Test
    void unknownPath_returns404() throws Exception {
        expectError(get("/test-errors/olmayan-adres"), 404, "Aradığınız kaynak bulunamadı.");
    }

    @Test
    void wrongHttpMethod_returns405WithAllowHeader() throws Exception {
        expectError(get("/test-errors/validation"), 405, "Bu adres için GET yöntemi desteklenmiyor.")
                .andExpect(header().string("Allow", containsString("POST")));
    }

    @Test
    void springUploadLimit_returns413() throws Exception {
        expectError(get("/test-errors/spring-upload-limit"), 413, "Dosya boyutu izin verilen sınırı aşıyor.");
    }

    @Test
    void unsupportedContentType_returns415() throws Exception {
        mockMvc.perform(post("/test-errors/validation").contentType(MediaType.TEXT_PLAIN).content("düz metin"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value("Desteklenmeyen içerik türü."));
    }

    // ---------------------------------------------------------------- yardımcılar

    /** Durum kodu, gövdedeki status, message ve boş errors nesnesini birlikte doğrular. */
    private ResultActions expectError(
            org.springframework.test.web.servlet.RequestBuilder request, int status, String message) throws Exception {
        return mockMvc.perform(request)
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors", aMapWithSize(0)));
    }

    record SampleRequest(
            @NotBlank(message = "Ad zorunludur.") String name,
            @NotBlank(message = "E-posta zorunludur.") @Email(message = "Geçerli bir e-posta adresi girin.") String email,
            @Size(min = 8, message = "Parola en az 8 karakter olmalıdır.") String password) {
    }

    @RestController
    @RequestMapping("/test-errors")
    static class TestEndpoints {

        @PostMapping("/validation")
        String validation(@Valid @RequestBody SampleRequest request) {
            return "ok";
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Kurs bulunamadı.");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Bu kursu düzenleme yetkiniz yok.");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Bu kursa zaten kayıtlısınız.");
        }

        @GetMapping("/conflict-email")
        void conflictEmail() {
            throw new ConflictException("Bu e-posta adresi zaten kayıtlı.",
                    java.util.Map.of("email", "Bu e-posta adresi zaten kayıtlı."));
        }

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("Her soruda en az bir doğru şık olmalıdır.");
        }

        @GetMapping("/too-large")
        void tooLarge() {
            throw new PayloadTooLargeException("Video en fazla 200 MB olabilir.");
        }

        @GetMapping("/unsupported-type")
        void unsupportedType() {
            throw new UnsupportedMediaTypeException("Yalnız mp4 ve webm videolar yüklenebilir.");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException(SECRET);
        }

        @GetMapping("/typed")
        String typed(@RequestParam int page) {
            return "page " + page;
        }

        @GetMapping("/spring-upload-limit")
        void springUploadLimit() {
            throw new MaxUploadSizeExceededException(1024);
        }
    }
}
