package com.example.LibraryManagement_Monolithic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;

    @Column(length = 100)
    private String emailTarget;

    private LocalDateTime expiryDate;

    @Builder.Default
    private Boolean used = false;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum TokenType {
        EMAIL_VERIFICATION,
        RESET_PASSWORD,
        CHANGE_EMAIL
    }
}