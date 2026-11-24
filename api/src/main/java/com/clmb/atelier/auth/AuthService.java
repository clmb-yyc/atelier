package com.clmb.atelier.auth;

import com.clmb.atelier.config.WebSecurityConfig;
import com.clmb.atelier.users.User;
import com.clmb.atelier.users.UserRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final UserRepo users;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AuthService(UserRepo users, PasswordEncoder passwords,
                       AuthenticationManager authenticationManager, JwtEncoder jwtEncoder) {
        this.users = users;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    public User signup(String username, String password) {
        if (username == null || !username.matches("[A-Za-z0-9._-]{3,64}")
                || !validPassword(password)
                || password.codePointCount(0, password.length()) < 8) {
            throw new InvalidInputException("Please check your username and password and try again.");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPassword(passwords.encode(password));
        user.setRole(User.Role.VIEWER);

        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (users.findByUsername(username) != null) {
                throw new UsernameAlreadyExistsException();
            }
            throw exception;
        }
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || !validPassword(password)) {
            throw new InvalidInputException("Please check your username and password and try again.");
        }

        Authentication user = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(WebSecurityConfig.JWT_ISSUER)
                .subject(user.getName())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .claim(WebSecurityConfig.AUTHORITIES_CLAIM, user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new LoginResult(token, TOKEN_TTL.toSeconds());
    }

    private boolean validPassword(String password) {
        return password != null && !password.isEmpty() && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public record LoginResult(String token, long expiresIn) {}

    static class InvalidInputException extends RuntimeException {
        InvalidInputException(String message) {
            super(message);
        }
    }

    static class UsernameAlreadyExistsException extends RuntimeException {
        UsernameAlreadyExistsException() {
            super("We couldn't create an account with those details.");
        }
    }
}
