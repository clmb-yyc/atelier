package com.clmb.atelier.auth;

import com.clmb.atelier.config.WebSecurityConfig;
import com.clmb.atelier.users.User;
import com.clmb.atelier.users.UserRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final UserRepo users;
    private final RefreshTokenRepo refreshTokens;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepo users, RefreshTokenRepo refreshTokens, PasswordEncoder passwords,
                       AuthenticationManager authenticationManager, JwtEncoder jwtEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
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

    @Transactional
    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || !validPassword(password)) {
            throw new InvalidInputException("Please check your username and password and try again.");
        }

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        User user = users.findByUsername(authentication.getName());
        if (user == null) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public LoginResult refresh(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken stored = refreshTokens.findByTokenHash(hashToken(token))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!stored.getExpiresAt().isAfter(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }
        User user = users.findById(stored.getUserId()).orElseThrow(InvalidRefreshTokenException::new);

        // The lock, deletion, and new token are part of one transaction.
        refreshTokens.delete(stored);
        return issueTokens(user);
    }

    private LoginResult issueTokens(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(WebSecurityConfig.JWT_ISSUER)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .claim(WebSecurityConfig.AUTHORITIES_CLAIM, new UserPrincipal(user).getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        refreshTokens.save(new RefreshToken(hashToken(refreshToken), user.getId(), now.plus(REFRESH_TOKEN_TTL)));
        return new LoginResult(accessToken, TOKEN_TTL.toSeconds(), refreshToken, REFRESH_TOKEN_TTL.toSeconds());
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean validPassword(String password) {
        return password != null && !password.isEmpty() && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

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

    static class InvalidRefreshTokenException extends RuntimeException {}
}
