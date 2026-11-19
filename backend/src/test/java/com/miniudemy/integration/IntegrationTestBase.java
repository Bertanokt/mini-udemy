package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Uçtan uca testlerin ortak tabanı (#65). Uygulama gerçek bir portta açılır, istekler gerçek HTTP ile gider, veriler
 * Docker'da çalışan gerçek bir PostgreSQL 17'ye yazılır. Şemayı uygulamanın kendi Flyway migration'ları kurar.
 *
 * <ul>
 *   <li>Konteyner JVM başına bir kez açılır (statik blokta, {@code @Container} yok); tüm {@code *IT} sınıfları ve
 *       Spring'in önbelleğe aldığı aynı uygulama bağlamı onu paylaşır. Bağlantı ayarlarını {@link ServiceConnection}
 *       verir, adres elle yazılmaz.</li>
 *   <li>Yüklenen dosyalar ({@code app.storage.root}) her çalıştırmada yeni bir geçici dizine yazılır.</li>
 *   <li>Her testten önce tablolar boşaltılır (migration'ın eklediği kategoriler kalır); testler birbirinin verisini
 *       görmez ve sıraları önemli değildir.</li>
 * </ul>
 *
 * Alt sınıflar adını {@code *IT} ile bitirir: bunları {@code ./mvnw verify} içinde failsafe çalıştırır, birim
 * testleri ({@code *Test}) surefire'da kalır. Çalışmak için Docker gerekir (CI'daki ubuntu-latest'te vardır).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        // Tek konteyner deseni: JVM sonunda Testcontainers (Ryuk) kapatır.
        POSTGRES.start();
    }

    private static final Path STORAGE_ROOT = createStorageRoot();

    /** Migration'ın kurduğu ve testlerin silmemesi gereken tablolar. */
    private static final List<String> KEPT_TABLES = List.of("flyway_schema_history", "categories");

    @Autowired
    protected TestRestTemplate http;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root", STORAGE_ROOT::toString);
    }

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("mini-udemy-it-uploads");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class).stream()
                .filter(t -> !KEPT_TABLES.contains(t))
                .toList();
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    // --- Kullanıcılar ---

    /** Kayıt ucuyla (POST /api/auth/register) öğrenci ya da eğitmen açar, giriş yapıp token'ını döner. */
    protected String registerAndLogin(String fullName, String email, String role) {
        ResponseEntity<JsonNode> registered = post("/api/auth/register", null,
                Map.of("fullName", fullName, "email", email, "password", "Parola123!", "role", role));
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return login(email, "Parola123!");
    }

    /** Yönetici kayıt ucundan açılamaz (kural gereği 400); bu yüzden doğrudan veritabanına eklenir. */
    protected String createAdminAndLogin(String email) {
        jdbc.update("INSERT INTO users (email, password_hash, full_name, role) VALUES (?, ?, ?, 'ADMIN')",
                email, passwordEncoder.encode("Parola123!"), "Ali Yönetici");
        return login(email, "Parola123!");
    }

    protected String login(String email, String password) {
        ResponseEntity<JsonNode> response = post("/api/auth/login", null,
                Map.of("email", email, "password", password));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().get("accessToken").asText();
    }

    // --- Dosyalar ---

    /**
     * Yüklenmiş bir dosyayı taklit eder: içeriği geçici depolama dizinine yazar ve {@code stored_files} kaydını
     * ekler (anahtar, uygulamanın ürettiği gibi bir UUID). Yükleme ucunu sınamayan senaryolar için.
     */
    protected long storeFile(long uploaderId, String originalName, String mimeType, byte[] content) {
        String key = UUID.randomUUID().toString();
        try {
            Files.write(STORAGE_ROOT.resolve(key), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return jdbc.queryForObject("""
                INSERT INTO stored_files (uploader_id, storage_key, original_name, mime_type, size_bytes)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, Long.class, uploaderId, key, originalName, mimeType, content.length);
    }

    /** Depolama dizininde bu anahtarla bir dosya var mı (yükleme ve silme testleri için). */
    protected boolean fileOnDisk(String storageKey) {
        return Files.isRegularFile(STORAGE_ROOT.resolve(storageKey));
    }

    // --- HTTP ---

    /** Gerçek multipart yükleme (alan adı {@code file}); {@code token} null ise girişsiz. */
    protected ResponseEntity<JsonNode> upload(String path, String token, String fileName, byte[] content) {
        HttpHeaders headers = headers(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    /** Dosya indiren uçlar için: gövde bayt olarak döner; {@code range} null değilse Range başlığı gönderilir. */
    protected ResponseEntity<byte[]> download(String path, String token, String range) {
        HttpHeaders headers = headers(token);
        if (range != null) {
            headers.set(HttpHeaders.RANGE, range);
        }
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
    }

    protected ResponseEntity<JsonNode> get(String path, String token) {
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> post(String path, String token, Object body) {
        return http.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> put(String path, String token, Object body) {
        return http.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, headers(token)), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> delete(String path, String token) {
        return http.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers(token)), JsonNode.class);
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
