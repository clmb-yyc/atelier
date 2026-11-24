package com.clmb.atelier.auth;

import com.clmb.atelier.users.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<RegisteredUser> signup(@RequestBody Credentials credentials) {
        User user = authService.signup(credentials.username(), credentials.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisteredUser(user.getId(), user.getUsername(), user.getRole()));
    }

    @PostMapping("/login")
    public AccessToken login(@RequestBody Credentials credentials) {
        AuthService.LoginResult result = authService.login(credentials.username(), credentials.password());
        return new AccessToken(result.token(), "Bearer", result.expiresIn());
    }

    @ExceptionHandler(AuthService.InvalidInputException.class)
    ResponseEntity<Map<String, String>> invalidInput(AuthService.InvalidInputException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(AuthService.UsernameAlreadyExistsException.class)
    ResponseEntity<Map<String, String>> signupConflict(AuthService.UsernameAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, String>> badCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "We couldn't sign you in. Check your username and password."));
    }
}
