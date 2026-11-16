package com.josue.reservas.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * - Consultas (GET) públicas; crear y cancelar exigen un JWT (Authorization: Bearer ...).
 * - Sin sesión ni cookies (API stateless), por eso CSRF no aplica y se desactiva.
 * - El token lo emite /api/auth/login y se firma con HS256 usando el secreto de la configuración.
 */
@Configuration
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers("/h2-console/**", "/error").permitAll()
                        // Documentación OpenAPI y Swagger UI: públicas, como las consultas
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                // La consola de H2 (sólo desarrollo) se muestra dentro de un iframe del mismo origen
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder(); // bcrypt por defecto
    }

    /** Un único usuario configurable. Con más tiempo: usuarios en BD con roles. */
    @Bean
    UserDetailsService usuarios(PropiedadesSeguridad props, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername(props.usuario())
                .password(encoder.encode(props.clave()))
                .roles("ADMIN")
                .build());
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService usuarios, PasswordEncoder encoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarios);
        proveedor.setPasswordEncoder(encoder);
        return new ProviderManager(proveedor);
    }

    @Bean
    JwtEncoder jwtEncoder(PropiedadesSeguridad props) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(props)));
    }

    @Bean
    JwtDecoder jwtDecoder(PropiedadesSeguridad props) {
        return NimbusJwtDecoder.withSecretKey(clave(props)).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static SecretKey clave(PropiedadesSeguridad props) {
        return new SecretKeySpec(props.jwtSecreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
