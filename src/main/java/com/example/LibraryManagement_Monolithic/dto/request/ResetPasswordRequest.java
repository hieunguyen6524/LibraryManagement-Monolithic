package com.example.LibraryManagement_Monolithic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "Please provide new password!")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Please provide password confirm!")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String passwordConfirm;

    public void validate() {
        if (!newPassword.equals(passwordConfirm)) {
            throw new IllegalArgumentException("Passwords do not match!");
        }
    }
}
