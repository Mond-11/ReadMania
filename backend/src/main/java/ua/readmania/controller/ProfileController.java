package ua.readmania.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.dto.auth.UserResponse;
import ua.readmania.dto.user.ChangePasswordRequest;
import ua.readmania.dto.user.UpdateProfileRequest;
import ua.readmania.service.UserService;

@RestController
@RequestMapping({"/api/account", "/api/profile"})
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Principal principal) {
        UserResponse response = userService.getProfile(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(Principal principal,
                                                      @Valid @RequestBody UpdateProfileRequest request,
                                                      HttpServletRequest httpRequest,
                                                      HttpServletResponse httpResponse) {
        UserResponse response = userService.updateProfile(principal.getName(), request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<UserResponse> changePassword(Principal principal,
                                                       @Valid @RequestBody ChangePasswordRequest request) {
        UserResponse response = userService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(response);
    }
}
