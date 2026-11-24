package com.clmb.atelier.config;

import com.clmb.atelier.auth.MyUserDetailsService;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    // Identifies this application in the token's "iss" claim, which Spring checks when validating a JWT.
    public static final String JWT_ISSUER = "clmb-atelier";

    // Names the claim containing the user's authorities, such as ROLE_VIEWER or ROLE_STAFF.
    public static final String AUTHORITIES_CLAIM = "authorities";

    @Bean
    PasswordEncoder passwordEncoder() {
        // New passwords and upgraded passwords are stored with the {bcrypt} prefix.
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(
                "bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder()));

        // Legacy passwords have no recognized {id} prefix, so this fallback compares them as plaintext.
        // After a successful match, DaoAuthenticationProvider hashes the submitted password with Bcrypt
        // and calls MyUserDetailsService.updatePassword() to save the new hash.
        encoder.setDefaultPasswordEncoderForMatches(NoOpPasswordEncoder.getInstance());
        return encoder;
    }

    @Bean
    AuthenticationManager authenticationManager(MyUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsPasswordService(userDetailsService);
        return new ProviderManager(provider);
    }

    @Bean
    SecretKey jwtKey(@Value("${atelier.jwt.secret}") String secret) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("ATELIER_JWT_SECRET must be at least 32 bytes encoded as Base64");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return NimbusJwtEncoder.withSecretKey(jwtKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JWT_ISSUER));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AUTHORITIES_CLAIM);
        authorities.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthentication = new JwtAuthenticationConverter();
        jwtAuthentication.setJwtGrantedAuthoritiesConverter(authorities);

        BearerTokenResolver bearerTokens = new DefaultBearerTokenResolver();
        PathPatternRequestMatcher refreshRequest = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/refresh");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(refreshRequest).permitAll()
                        .requestMatchers("/api/users/**").hasRole("STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").hasAnyRole("VIEWER", "STAFF")
                        .requestMatchers("/api/locations/**").hasRole("STAFF")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(request -> {
                            // The refresh endpoint authenticates with the refresh token in its request body.
                            if (refreshRequest.matches(request)) {
                                return null;
                            }
                            return bearerTokens.resolve(request);
                        })
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthentication)))
                .build();
    }
}
