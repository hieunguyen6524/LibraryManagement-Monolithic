package com.example.LibraryManagement_Monolithic.service;

import com.example.LibraryManagement_Monolithic.dto.response.LoginResponse;
import com.example.LibraryManagement_Monolithic.dto.response.SignupResponse;
import com.example.LibraryManagement_Monolithic.dto.response.TokenResponse;
import com.example.LibraryManagement_Monolithic.dto.response.UserResponse;
import com.example.LibraryManagement_Monolithic.entity.*;
import com.example.LibraryManagement_Monolithic.exception.BadRequestException;
import com.example.LibraryManagement_Monolithic.exception.NotFoundException;
import com.example.LibraryManagement_Monolithic.exception.UnauthorizedException;
import com.example.LibraryManagement_Monolithic.repository.*;
import com.example.LibraryManagement_Monolithic.security.CustomUserDetails;
import com.example.LibraryManagement_Monolithic.security.JwtService;
import com.example.LibraryManagement_Monolithic.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
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
    private final MailService mailService;

    private final long refreshExpirationMs = 604800000L;

    @Transactional
    public SignupResponse signup(String email, String password) {
        if (userRepo.existsByEmail(email)) {
            throw new BadRequestException("Email existed");
        }

        User u = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .isVerified(false)
                .isActive(true)
                .roles(new HashSet<>())
                .build();

        Role userRole = roleRepo.findByRoleName("USER")
                .orElseThrow(() -> new NotFoundException("Role USER not found"));
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

        mailService.sendVerificationEmail(email, token);

        return SignupResponse.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .roles(u.getRoles().stream().map(Role::getRoleName).toList())
                .isVerified(u.getIsVerified())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public Pair<LoginResponse, String> login(String email, String password) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        User user = userRepo.findById(userDetails.getUserId()).orElseThrow();

        if (!user.getIsVerified()) {
            throw new UnauthorizedException("Email not verified. Please verify before login.");
        }

        String accessToken = jwtService.generateToken(email, roles);
        String refreshId = UUID.randomUUID().toString();

        RefreshContext rc = RefreshContext.builder()
                .contextId(refreshId)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();
        refreshRepo.save(rc);


        UserResponse userResponse = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .roles(roles)
                .isVerified(user.getIsVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .user(userResponse)
                .build();


        return Pair.of(loginResponse, refreshId);
    }


    @Transactional
    public TokenResponse refreshAccessToken(String refreshId) {
        if (blacklistedRepo.existsByToken(refreshId)) {
            throw new RuntimeException("Refresh token is blacklisted");
        }

        RefreshContext rc = refreshRepo.findByContextId(refreshId)
                .orElseThrow(() -> new NotFoundException("Refresh token not found"));

        if (rc.getRevoked() || rc.getExpiryDate().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Refresh token invalid");


        List<String> roles = rc.getUser().getRoles().stream().map(r -> "ROLE_" + r.getRoleName()).toList();
        String accessToken = jwtService.generateToken(rc.getUser().getEmail(), roles);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .build();
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
                .orElseThrow(() -> new NotFoundException("Token not found"));
        if (vt.getUsed()) throw new RuntimeException("Token already used");
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) throw new RuntimeException("Token expired");

        User u = vt.getUser();
        u.setIsVerified(true);
        userRepo.save(u);

        vt.setUsed(true);
        verificationRepo.save(vt);
    }

    @Transactional
    public void forgotPassword(String email) {

        User user = userRepo.findByEmail(email).orElseThrow(() -> new NotFoundException("Email not found"));

        if (!user.getIsVerified()) {
            throw new UnauthorizedException("Email not verified. Please verify your email before resetting password.");
        }

        String token = UUID.randomUUID().toString();


        String hashed = TokenHasher.hashSHA256(token);

        VerificationToken verificationToken = VerificationToken.builder()
                .token(hashed)
                .type(VerificationToken.TokenType.RESET_PASSWORD)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .user(user)
                .emailTarget(email)
                .used(false)
                .build();
        verificationRepo.save(verificationToken);
        mailService.sendResetPasswordEmail(email, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String passwordConfirm) {
        String hashed = TokenHasher.hashSHA256(token);

        VerificationToken verificationToken = verificationRepo.findByToken(hashed).orElseThrow(() -> new NotFoundException("Token not found"));

        if (verificationToken.getUsed()) {
            throw new BadRequestException("Token has already been used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        verificationToken.setUsed(true);
        verificationRepo.save(verificationToken);
        User user = verificationToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));


        userRepo.save(user);
    }

}


