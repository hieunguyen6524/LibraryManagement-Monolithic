package com.example.LibraryManagement_Monolithic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Please provide email!")
    @Email(message = "Invalid email!")
    private String email;

    @NotBlank(message = "Please provide password!")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
