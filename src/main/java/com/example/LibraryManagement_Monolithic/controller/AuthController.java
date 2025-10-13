package com.example.LibraryManagement_Monolithic.controller;

import com.example.LibraryManagement_Monolithic.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        var res = authService.signup(body.get("email"), body.get("password"));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        var res = authService.login(body.get("email"), body.get("password"));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        var res = authService.refreshAccessToken(body.get("refreshToken"));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, @RequestBody(required = false) Map<String, String> body) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) token = authHeader.substring(7);
        String refresh = body != null ? body.get("refreshToken") : null;
        authService.logout(token, refresh);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }
}
