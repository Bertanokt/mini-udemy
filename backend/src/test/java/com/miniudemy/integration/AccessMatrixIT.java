package com.miniudemy.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * Rol × uç erişim matrisi (#139, proje dokümanı §5–6). Beklenen roller sözleşmeden okunur
 * ({@code docs/api/openapi.yaml} › her işlemin {@code x-roles}'u); kodda uygulanmış her uç için:
 *
 * <ol>
 *   <li><b>Sözleşme = kod:</b> ucun kodda beklediği roller ({@code /api/admin/**} ve {@code /api/instructor/**} adres
 *       kuralı, yoksa {@code @PreAuthorize}, ikisi de yoksa "girişli herkes") {@code x-roles} ile aynı.</li>
 *   <li><b>Girişsiz:</b> korumalı uç 401; herkese açık uç 401/403 vermez.</li>
 *   <li><b>Yanlış rol:</b> gövde almayan uçlarda, {@code x-roles}'ta olmayan her rol gerçek token'la 403 alır. Gövdeli
 *       uçlarda Spring gövdeyi yetkiden önce doğruladığı için (geçersiz gövde 400) bu adım yerine 1. madde geçerli.</li>
 * </ol>
 *
 * Sözleşmede olup kodda henüz olmayan uçlar (ör. sertifika görüntüleme #130) atlanır; uygulanınca kendiliğinden
 * matrise girer.
 */
class AccessMatrixIT extends IntegrationTestBase {

    private static final List<String> ROLES = List.of("STUDENT", "INSTRUCTOR", "ADMIN");
    private static final Set<String> ANY_ROLE = new LinkedHashSet<>(ROLES);
    private static final Set<String> PUBLIC = Set.of("PUBLIC");
    private static final Pattern ROLE_IN_EXPRESSION = Pattern.compile("'([A-Z]+)'");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}]+)}");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    /** Sözleşmedeki bir işlem ve onu karşılayan controller metodu. */
    private record Endpoint(HttpMethod method, String path, Set<String> contractRoles, HandlerMethod handler) {

        String name() {
            return method + " " + path;
        }

        boolean takesBody() {
            for (MethodParameter parameter : handler.getMethodParameters()) {
                if (parameter.hasParameterAnnotation(RequestBody.class)
                        || parameter.hasParameterAnnotation(RequestPart.class)
                        || MultipartFile.class.isAssignableFrom(parameter.getParameterType())) {
                    return true;
                }
            }
            return false;
        }

        /** Kodun beklediği roller: adres kuralı, yoksa @PreAuthorize, yoksa girişli herkes. */
        Set<String> codeRoles() {
            if (path.startsWith("/api/admin/")) {
                return Set.of("ADMIN");
            }
            if (path.startsWith("/api/instructor/")) {
                return Set.of("INSTRUCTOR");
            }
            PreAuthorize pre = AnnotatedElementUtils.findMergedAnnotation(handler.getMethod(), PreAuthorize.class);
            if (pre == null) {
                pre = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), PreAuthorize.class);
            }
            if (pre == null) {
                return ANY_ROLE;
            }
            Set<String> roles = new LinkedHashSet<>();
            Matcher matcher = ROLE_IN_EXPRESSION.matcher(pre.value());
            while (matcher.find()) {
                roles.add(matcher.group(1));
            }
            return roles;
        }

        /** Yol değişkenleri var olmayan bir değerle doldurulur; yetki kararı kayda bakmadan verilmeli. */
        String concretePath() {
            return PATH_VARIABLE.matcher(path).replaceAll(m -> m.group(1).equals("code") ? "AAAA-AAAA-AA" : "999999");
        }
    }

    @Test
    void contractRoles_matchTheRolesTheCodeEnforces_forEveryImplementedEndpoint() {
        List<String> mismatches = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            if (endpoint.contractRoles().equals(PUBLIC)) {
                continue; // herkese açık uçlar ikinci testte istekle denetlenir
            }
            Set<String> code = endpoint.codeRoles();
            if (!code.equals(endpoint.contractRoles())) {
                mismatches.add(endpoint.name() + ": sözleşme " + new TreeSet<>(endpoint.contractRoles()) + ", kod "
                        + new TreeSet<>(code));
            }
        }
        assertThat(mismatches).as("x-roles ile kodun yetki kuralı farklı olan uçlar").isEmpty();
    }

    @TestFactory
    Stream<DynamicTest> anonymousAndWrongRoleRequests_areRefused() {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("STUDENT", registerAndLogin("Ayşe Yılmaz", "ayse@example.com", "STUDENT"));
        tokens.put("INSTRUCTOR", registerAndLogin("Mehmet Kaya", "mehmet@example.com", "INSTRUCTOR"));
        tokens.put("ADMIN", createAdminAndLogin("admin@example.com"));

        List<DynamicTest> tests = new ArrayList<>();
        for (Endpoint endpoint : endpoints()) {
            String url = endpoint.concretePath();
            if (endpoint.contractRoles().equals(PUBLIC)) {
                tests.add(DynamicTest.dynamicTest(endpoint.name() + " · girişsiz açık", () ->
                        assertThat(status(endpoint.method(), url, null).value()).as(endpoint.name())
                                .isNotIn(401, 403)));
                continue;
            }
            tests.add(DynamicTest.dynamicTest(endpoint.name() + " · girişsiz 401", () ->
                    assertThat(status(endpoint.method(), url, null)).as(endpoint.name())
                            .isEqualTo(HttpStatus.UNAUTHORIZED)));
            if (endpoint.takesBody()) {
                continue;
            }
            for (String role : ROLES) {
                if (!endpoint.contractRoles().contains(role)) {
                    tests.add(DynamicTest.dynamicTest(endpoint.name() + " · " + role + " 403", () ->
                            assertThat(status(endpoint.method(), url, tokens.get(role)))
                                    .as(endpoint.name() + " / " + role).isEqualTo(HttpStatus.FORBIDDEN)));
                }
            }
        }
        assertThat(tests).as("matriste en az birkaç korumalı uç olmalı").hasSizeGreaterThan(50);
        return tests.stream();
    }

    private HttpStatusCode status(HttpMethod method, String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(url, method, new HttpEntity<>(headers), String.class).getStatusCode();
    }

    /** Sözleşmedeki x-roles'lu işlemlerden kodda uygulanmış olanlar. */
    private List<Endpoint> endpoints() {
        Map<String, HandlerMethod> implemented = new LinkedHashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            for (var method : info.getMethodsCondition().getMethods()) {
                for (String pattern : info.getPatternValues()) {
                    implemented.put(method.name() + " " + normalize(pattern), entry.getValue());
                }
            }
        }
        List<Endpoint> endpoints = new ArrayList<>();
        contract().forEach((operation, roles) -> {
            String[] parts = operation.split(" ", 2);
            HandlerMethod handler = implemented.get(parts[0] + " " + normalize(parts[1]));
            if (handler != null) {
                endpoints.add(new Endpoint(HttpMethod.valueOf(parts[0]), parts[1], roles, handler));
            }
        });
        return endpoints;
    }

    private static String normalize(String path) {
        return PATH_VARIABLE.matcher(path).replaceAll("{}");
    }

    /** "GET /api/..." → x-roles; x-roles'u olmayan işlemler (ör. auth uçları) matrise girmez. */
    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> contract() {
        Map<String, Object> spec;
        try (InputStream in = Files.newInputStream(Path.of("..", "docs", "api", "openapi.yaml"))) {
            spec = new Yaml().load(in);
        } catch (IOException e) {
            throw new IllegalStateException("docs/api/openapi.yaml okunamadı", e);
        }
        Map<String, Set<String>> operations = new LinkedHashMap<>();
        Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) spec.get("paths");
        paths.forEach((path, item) -> item.forEach((method, operation) -> {
            if (operation instanceof Map<?, ?> op && op.get("x-roles") instanceof List<?> roles) {
                operations.put(method.toUpperCase() + " " + path,
                        new LinkedHashSet<>(roles.stream().map(Object::toString).toList()));
            }
        }));
        return operations;
    }
}
