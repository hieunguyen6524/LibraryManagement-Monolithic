package com.example.LibraryManagement_Monolithic.controller.user;

import com.example.LibraryManagement_Monolithic.dto.request.ChangeEmailRequest;
import com.example.LibraryManagement_Monolithic.dto.response.ApiResponse;
import com.example.LibraryManagement_Monolithic.entity.User;
import com.example.LibraryManagement_Monolithic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("")
    public ResponseEntity<?> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.status(200).body(ApiResponse.success("Get current user", user));
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody ChangeEmailRequest request) {
        userService.requestEmailChange(request.getEmail());
        return ResponseEntity.status(200).body(ApiResponse.success("Please check your new email!"));
    }

    @GetMapping("/verify-change-email")
    public ResponseEntity<?> verifyChangeEmail(@RequestParam String token) {
        userService.confirmEmailChange(token);
        return ResponseEntity.ok(ApiResponse.success("Email changed successfully"));
    }
}
