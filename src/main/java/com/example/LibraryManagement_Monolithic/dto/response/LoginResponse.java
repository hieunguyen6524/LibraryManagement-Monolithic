package com.example.LibraryManagement_Monolithic.dto.response;

import com.example.LibraryManagement_Monolithic.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private User user;
}
