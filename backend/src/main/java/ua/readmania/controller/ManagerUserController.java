package ua.readmania.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.readmania.domain.user.UserStatus;
import ua.readmania.dto.manager.user.BlockUserRequest;
import ua.readmania.dto.manager.user.DeleteInactiveUsersRequest;
import ua.readmania.dto.manager.user.InactiveUserResponse;
import ua.readmania.dto.manager.user.ManagerUserDetailsResponse;
import ua.readmania.dto.manager.user.ManagerUnblockRequestResponse;
import ua.readmania.dto.manager.user.ManagerUserSummaryResponse;
import ua.readmania.dto.manager.user.UnblockUsersRequest;
import ua.readmania.service.ManagerInactiveUserService;
import ua.readmania.service.ManagerUserService;

@RestController
@RequestMapping("/api/manager/users")
public class ManagerUserController {

    private final ManagerUserService managerUserService;
    private final ManagerInactiveUserService managerInactiveUserService;

    public ManagerUserController(ManagerUserService managerUserService,
                                 ManagerInactiveUserService managerInactiveUserService) {
        this.managerUserService = managerUserService;
        this.managerInactiveUserService = managerInactiveUserService;
    }

    @GetMapping
    public ResponseEntity<Page<ManagerUserSummaryResponse>> getUsers(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "status", required = false) UserStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ManagerUserSummaryResponse> response = managerUserService.getUsers(email, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerUserDetailsResponse> getUserDetails(@PathVariable("id") Long id) {
        ManagerUserDetailsResponse response = managerUserService.getUserDetails(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ManagerUserDetailsResponse> blockUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody BlockUserRequest request) {
        ManagerUserDetailsResponse response = managerUserService.blockUser(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unblock-requests")
    public ResponseEntity<List<ManagerUnblockRequestResponse>> getUnblockRequests() {
        List<ManagerUnblockRequestResponse> response = managerUserService.getUnblockRequests();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unblock")
    public ResponseEntity<Map<String, String>> unblockUsers(
            @Valid @RequestBody UnblockUsersRequest request) {
        managerUserService.unblockUsers(request);
        return ResponseEntity.ok(Map.of("message", "Облікові записи успішно розблоковано"));
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<InactiveUserResponse>> getInactiveUsers() {
        List<InactiveUserResponse> response = managerInactiveUserService.getInactiveUsers();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/inactive")
    public ResponseEntity<Map<String, String>> deleteInactiveUsers(
            @Valid @RequestBody DeleteInactiveUsersRequest request) {
        managerInactiveUserService.deleteInactiveUsers(request);
        return ResponseEntity.ok(Map.of("message", "Неактивні облікові записи успішно видалено"));
    }
}
