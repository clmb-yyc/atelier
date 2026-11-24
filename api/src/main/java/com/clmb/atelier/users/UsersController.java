package com.clmb.atelier.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserRepo users;

    public UsersController(UserRepo users) {
        this.users = users;
    }

    @GetMapping
    public List<UserSummary> listUsers() {
        return users.findAll().stream()
                .map(user -> new UserSummary(user.getId(), user.getUsername(), user.getRole()))
                .toList();
    }

    public record UserSummary(UUID id, String username, User.Role role) {}
}
