package com.example.LibraryManagement_Monolithic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    private String reason;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
