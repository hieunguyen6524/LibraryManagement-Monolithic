package com.example.LibraryManagement_Monolithic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String email;
    private List<String> roles;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}