package com.josue.reservas.seguridad;

import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final PropiedadesSeguridad props;

    public AuthController(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, PropiedadesSeguridad props) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    /** Credenciales incorrectas -> AuthenticationException -> 401 (ver ManejadorErrores). */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest datos) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(datos.usuario(), datos.clave()));
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("reservas-salas")
                .subject(auth.getName())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(props.jwtDuracion()))
                .claim("roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_")).toList()) // Spring Security 7 añade FACTOR_*
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", props.jwtDuracion().toSeconds());
    }
}
