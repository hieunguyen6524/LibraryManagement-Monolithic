package com.example.LibraryManagement_Monolithic.service;

import com.example.LibraryManagement_Monolithic.entity.User;
import com.example.LibraryManagement_Monolithic.entity.VerificationToken;
import com.example.LibraryManagement_Monolithic.exception.BadRequestException;
import com.example.LibraryManagement_Monolithic.exception.NotFoundException;
import com.example.LibraryManagement_Monolithic.exception.UnauthorizedException;
import com.example.LibraryManagement_Monolithic.repository.UserRepository;
import com.example.LibraryManagement_Monolithic.repository.VerificationTokenRepository;
import com.example.LibraryManagement_Monolithic.security.CustomUserDetails;
import com.example.LibraryManagement_Monolithic.util.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final MailService mailService;
    private final VerificationTokenRepository verificationTokenRepo;

    public User getCurrentUser() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepo.findById(userDetails.getUserId()).orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public void requestEmailChange(String newEmail) {
        User user = getCurrentUser();
        if (userRepo.existsByEmail(newEmail)) {
            throw new BadRequestException("Email existed");
        }
        String token = UUID.randomUUID().toString();
        String hashed = TokenHasher.hashSHA256(token);

        VerificationToken verificationToken = VerificationToken.builder()
                .token(hashed)
                .type(VerificationToken.TokenType.CHANGE_EMAIL)
                .expiryDate(LocalDateTime.now().plusMinutes(5))
                .user(user)
                .emailTarget(newEmail)
                .used(false)
                .build();

        verificationTokenRepo.save(verificationToken);

        mailService.sendChangeEmailVerification(newEmail, token);
    }

    public void confirmEmailChange(String token) {
        String hashed = TokenHasher.hashSHA256(token);

        VerificationToken verificationToken = verificationTokenRepo.findByToken(hashed).orElseThrow(() -> new NotFoundException("Not found toke!"));

        if (verificationToken.getUsed()) {
            throw new BadRequestException("Token has already been used");
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        verificationToken.setUsed(true);
        verificationTokenRepo.save(verificationToken);
        User user = verificationToken.getUser();
        user.setEmail(verificationToken.getEmailTarget());

        userRepo.save(user);
    }
}
