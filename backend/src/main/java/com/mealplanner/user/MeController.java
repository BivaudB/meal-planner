package com.mealplanner.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserService currentUserService;

    public MeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public MeResponse me() {
        AppUser user = currentUserService.currentUser();
        return new MeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
            currentUserService.isAdmin());
    }

    public record MeResponse(Long id, String username, String email, String displayName, boolean admin) {
    }
}
