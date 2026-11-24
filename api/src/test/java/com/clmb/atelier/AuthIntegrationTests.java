package com.clmb.atelier;

import com.clmb.atelier.users.User;
import com.clmb.atelier.users.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired UserRepo users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JsonMapper json;

    @BeforeEach
    void clearUsers() {
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

    private String login(String username, String password) throws Exception {
        String body = json.writeValueAsString(new Login(username, password));
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("accessToken").asString();
    }

    private record Login(String username, String password) {}
}
