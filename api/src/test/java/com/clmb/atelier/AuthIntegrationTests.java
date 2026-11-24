package com.clmb.atelier;

import com.clmb.atelier.auth.AuthService;
import com.clmb.atelier.auth.RefreshRequest;
import com.clmb.atelier.auth.RefreshTokenRepo;
import com.clmb.atelier.users.User;
import com.clmb.atelier.users.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AuthService authService;
    @Autowired UserRepo users;
    @Autowired RefreshTokenRepo refreshTokens;
    @Autowired JdbcTemplate database;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JsonMapper json;
    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearUsers() {
        refreshTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void signupLoginAndRoleChecks() throws Exception {
        String signup = """
                {"username":"climber", "password":"password123", "role":"STAFF"}
                """;
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        User user = users.findByUsername("climber");
        assertThat(user.getPasswordHash()).startsWith("{bcrypt}");
        assertThat(passwords.matches("password123", user.getPasswordHash())).isTrue();

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("We couldn't create an account with those details."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("We couldn't sign you in. Check your username and password."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"unknown\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("We couldn't sign you in. Check your username and password."));
        mvc.perform(get("/api/locations/calgary/routes"))
                .andExpect(status().isUnauthorized());

        String viewerToken = login("climber", "password123");
        mvc.perform(get("/api/locations/calgary/routes").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk()).andExpect(content().string("hello world"));
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/locations/calgary/routes").header("Authorization", "Bearer " + viewerToken + "broken"))
                .andExpect(status().isUnauthorized());

        user.setRole(User.Role.STAFF);
        users.saveAndFlush(user);
        String staffToken = login("climber", "password123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("climber"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void successfulLegacyLoginUpgradesPlaintextPassword() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("legacy");
        user.setPassword("old-password");
        user.setRole(User.Role.VIEWER);
        users.saveAndFlush(user);

        login("legacy", "old-password");
        String upgraded = users.findByUsername("legacy").getPasswordHash();
        assertThat(upgraded).startsWith("{bcrypt}");
        assertThat(passwords.matches("old-password", upgraded)).isTrue();
    }

    @Test
    void rejectsInvalidInputAndExpiredToken() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please check your username and password and try again."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please check your username and password and try again."));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new Login("climber", "a".repeat(73)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please check your username and password and try again."));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("clmb-atelier").subject("expired")
                .issuedAt(now.minusSeconds(180)).expiresAt(now.minusSeconds(120))
                .claim("authorities", List.of("ROLE_STAFF")).build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndUsesTheCurrentRole() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        JsonNode login = loginTokens("climber", "password123");
        String firstRefresh = login.get("refreshToken").asString();
        assertThat(login.get("refreshExpiresIn").asLong()).isEqualTo(604800);

        String storedHash = database.queryForObject("SELECT token_hash FROM refresh_tokens", String.class);
        assertThat(storedHash).matches("[0-9a-f]{64}").isNotEqualTo(firstRefresh);

        User user = users.findByUsername("climber");
        user.setRole(User.Role.STAFF);
        users.saveAndFlush(user);

        String response = mvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer expired-or-invalid")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody(firstRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        JsonNode refreshed = json.readTree(response);
        String nextRefresh = refreshed.get("refreshToken").asString();
        assertThat(nextRefresh).isNotEqualTo(firstRefresh);
        assertThat(database.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class)).isEqualTo(1);

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + refreshed.get("accessToken").asString()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(firstRefresh)))
                .andExpect(status().isUnauthorized());
        String secondResponse = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(nextRefresh)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(secondResponse).get("refreshToken").asString()).isNotEqualTo(nextRefresh);
    }

    @Test
    void rejectsExpiredAndInvalidRefreshTokens() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        JsonNode login = loginTokens("climber", "password123");
        String refreshToken = login.get("refreshToken").asString();

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(login.get("accessToken").asString())))
                .andExpect(status().isUnauthorized());

        database.update("UPDATE refresh_tokens SET expires_at = ?", Timestamp.from(Instant.now().minusSeconds(60)));
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your session has expired. Please sign in again."));
    }

    @Test
    void concurrentRefreshRequestsCanUseATokenOnlyOnce() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        String body = refreshBody(loginTokens("climber", "password123").get("refreshToken").asString());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> request = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start refresh requests");
                }
                return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
            assertThat(database.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class)).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void refreshRollsBackIfTheTransactionFails() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"climber\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        String token = loginTokens("climber", "password123").get("refreshToken").asString();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            authService.refresh(token);
            throw new IllegalStateException("Simulated failure before commit");
        })).isInstanceOf(IllegalStateException.class);

        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(token)))
                .andExpect(status().isOk());
        assertThat(database.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class)).isEqualTo(1);
    }

    private String login(String username, String password) throws Exception {
        return loginTokens(username, password).get("accessToken").asString();
    }

    private JsonNode loginTokens(String username, String password) throws Exception {
        String body = json.writeValueAsString(new Login(username, password));
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private String refreshBody(String token) {
        return json.writeValueAsString(new RefreshRequest(token));
    }

    private record Login(String username, String password) {}
}
