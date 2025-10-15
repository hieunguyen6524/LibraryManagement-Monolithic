package com.example.LibraryManagement_Monolithic.controller;

import com.example.LibraryManagement_Monolithic.dto.request.ForgotPasswordRequest;
import com.example.LibraryManagement_Monolithic.dto.request.LoginRequest;
import com.example.LibraryManagement_Monolithic.dto.request.ResetPasswordRequest;
import com.example.LibraryManagement_Monolithic.dto.request.SignupRequest;
import com.example.LibraryManagement_Monolithic.dto.response.ApiResponse;
import com.example.LibraryManagement_Monolithic.dto.response.LoginResponse;
import com.example.LibraryManagement_Monolithic.dto.response.SignupResponse;
import com.example.LibraryManagement_Monolithic.dto.response.TokenResponse;
import com.example.LibraryManagement_Monolithic.service.AuthService;
import com.example.LibraryManagement_Monolithic.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    public ResponseEntity<ApiResponse<Map<String, SignupResponse>>> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        request.validate();
        SignupResponse result = authService.signup(request.getEmail(), request.getPassword());
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Signup successful. Please check your email for verification link.", Map.of("user", result)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        var result = authService.login(request.getEmail(), request.getPassword());
        var data = result.getFirst();
        CookieUtil.addRefreshTokenCookie(response, result.getSecond(), 7 * 24 * 60 * 60);

        return ResponseEntity.status(200).body(ApiResponse.success("Login success", data));
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {

        var refreshOpt = CookieUtil.getRefreshTokenFromRequest(request);

        if (refreshOpt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Missing refresh token"));
        }

        TokenResponse accessToken = authService.refreshAccessToken(refreshOpt.get());

//        CookieUtil.addRefreshTokenCookie(response, refreshOpt.get(), 7 * 24 * 60 * 60);

        return ResponseEntity.status(201).body(ApiResponse.success("Get new access token", accessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        var refreshOpt = CookieUtil.getRefreshTokenFromRequest(request);

        if (refreshOpt.isEmpty()) {
            System.out.println("thang nay null");
        }


        authService.logout(accessToken, refreshOpt.orElse(null));

        CookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token, HttpServletResponse response) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.status(200).body(ApiResponse.success("Please check your email!"));
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request, @RequestParam String token) {
        request.validate();
        authService.resetPassword(token, request.getNewPassword(), request.getPasswordConfirm());
        return ResponseEntity.status(200).body(ApiResponse.success("Password is changed!"));
    }
}
