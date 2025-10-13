package com.example.LibraryManagement_Monolithic.service;

import com.example.LibraryManagement_Monolithic.entity.*;
import com.example.LibraryManagement_Monolithic.repository.*;
import com.example.LibraryManagement_Monolithic.security.CustomUserDetails;
import com.example.LibraryManagement_Monolithic.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final RefreshContextRepository refreshRepo;
    private final BlacklistedTokenRepository blacklistedRepo;
    private final VerificationTokenRepository verificationRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final long refreshExpirationMs = 604800000L;

    @Transactional
    public Map<String, String> signup(String email, String password) {
        if (userRepo.existsByEmail(email)) {
            throw new RuntimeException("Email existed");
        }

        User u = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .isVerified(false)
                .isActive(true)
                .roles(new HashSet<>())
                .build();
        Role userRole = roleRepo.findByRoleName("USER").orElseThrow(() -> new RuntimeException("Role USER not found"));


        u.getRoles().add(userRole);
        userRepo.save(u);

        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token)
                .type(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .user(u)
                .emailTarget(email)
                .used(false)
                .build();
        verificationRepo.save(vt);
        return Map.of("verificationToken", token);
    }

    public Map<String, String> login(String email, String password) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
        Authentication auth = authManager.authenticate(token);
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();

        String accessToken = jwtService.generateToken(email, roles);
        String refreshId = UUID.randomUUID().toString();
        RefreshContext rc = RefreshContext.builder()
                .contextId(refreshId)
                .user(userRepo.findById(userDetails.getUserId()).orElseThrow())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();
        refreshRepo.save(rc);

        return Map.of("accessToken", accessToken, "refreshToken", refreshId);
    }

    @Transactional
    public Map<String, String> refreshAccessToken(String refreshId) {
        RefreshContext rc = refreshRepo.findByContextId(refreshId)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        if (rc.getRevoked() || rc.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Refresh token invalid");

        // rotate: revoke old and create new refresh id
        rc.setRevoked(true);
        refreshRepo.save(rc);

        String newRefreshId = UUID.randomUUID().toString();
        RefreshContext newRc = RefreshContext.builder()
                .contextId(newRefreshId)
                .user(rc.getUser())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();
        refreshRepo.save(newRc);

        List<String> roles = rc.getUser().getRoles().stream().map(r -> "ROLE_" + r.getRoleName()).toList();
        String accessToken = jwtService.generateToken(rc.getUser().getEmail(), roles);

        return Map.of("accessToken", accessToken, "refreshToken", newRefreshId);
    }

    @Transactional
    public void logout(String accessToken, String refreshId) {
        if (accessToken != null && !blacklistedRepo.existsByToken(accessToken)) {
            BlacklistedToken bt = BlacklistedToken.builder()
                    .token(accessToken)
                    .reason("logout_access")
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.ofInstant(
                            jwtService.getExpirationDate(accessToken).toInstant(),
                            ZoneId.systemDefault()))
                    .build();
            blacklistedRepo.save(bt);
        }

        // 2️⃣ Blacklist refresh token (contextId)
        if (refreshId != null && !blacklistedRepo.existsByToken(refreshId)) {
            BlacklistedToken refreshBlacklisted = BlacklistedToken.builder()
                    .token(refreshId)
                    .reason("logout_refresh")
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();
            blacklistedRepo.save(refreshBlacklisted);
        }

        if (refreshId != null) {
            refreshRepo.findByContextId(refreshId).ifPresent(rc -> {
                rc.setRevoked(true);
                refreshRepo.save(rc);
            });
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        if (vt.getUsed()) throw new RuntimeException("Token already used");
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) throw new RuntimeException("Token expired");

        User u = vt.getUser();
        u.setIsVerified(true);
        userRepo.save(u);

        vt.setUsed(true);
        verificationRepo.save(vt);
    }
}
