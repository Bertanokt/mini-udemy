package com.miniudemy.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP güvenliği: durumsuz (stateless) JWT, sözleşmeye göre herkese açık uçlar, rol kuralları.
 *
 * <ul>
 *   <li>Token {@code Authorization: Bearer} başlığından Spring'in {@code BearerTokenAuthenticationFilter}'ı ile
 *       okunur ve {@code JwtDecoder} ile doğrulanır; kimliği {@link JwtAccountAuthenticationConverter} kurar.</li>
 *   <li>Oturum ve CSRF yok: token çerezde değil başlıkta taşınır. CORS izinleri {@code WebConfig}'ten gelir.</li>
 *   <li><b>Herkese açık uçlar</b> docs/api/openapi.yaml'da güvenliği boş ({@code security: []}) ya da isteğe
 *       bağlı olan 12 uçtur. İsteğe bağlı token'lı uçlarda ({@code GET /api/courses/{id}}, {@code .../sections})
 *       token gönderilirse yine doğrulanır; geçersizse 401 döner.</li>
 *   <li><b>Adres bazlı roller:</b> {@code /api/admin/**} yalnız ADMIN, {@code /api/instructor/**} yalnız INSTRUCTOR
 *       (sözleşmede bu öneklerin tüm uçları bu rolü ister). Diğer rol kuralları uçların kendisinde
 *       {@code @PreAuthorize} ile yazılır.</li>
 *   <li>Filtrede oluşan 401/403 {@link JsonSecurityErrorHandler} ile, controller içinde oluşan
 *       ({@code @PreAuthorize}) {@code GlobalExceptionHandler} ile aynı {@code ErrorResponse} gövdesinde döner.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** docs/api/openapi.yaml'da token istemeyen GET uçları. */
    static final String[] PUBLIC_GET = {
        "/api/health",
        "/actuator/health",
        "/api/categories",
        "/api/courses",
        "/api/courses/{id}",
        "/api/courses/{id}/cover",
        "/api/courses/{id}/sections",
        "/api/courses/{id}/reviews",
        "/api/certificates/verify/{code}",
    };

    /** docs/api/openapi.yaml'da token istemeyen POST uçları. */
    static final String[] PUBLIC_POST = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/password-reset/request",
        "/api/auth/password-reset/confirm",
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountLookup accountLookup,
            ObjectMapper objectMapper) throws Exception {
        JsonSecurityErrorHandler errorHandler = new JsonSecurityErrorHandler(objectMapper);
        JwtAccountAuthenticationConverter converter = new JwtAccountAuthenticationConverter(accountLookup);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/instructor/**").hasRole("INSTRUCTOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler));
        return http.build();
    }
}
