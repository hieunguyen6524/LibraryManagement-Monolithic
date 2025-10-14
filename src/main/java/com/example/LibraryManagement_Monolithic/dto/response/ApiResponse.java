package com.example.LibraryManagement_Monolithic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private TypeStatus status;
    private String message;
    private T data;

    public enum TypeStatus {
        success,
        error
    }

    // helper static methods
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .status(TypeStatus.success)
                .message(message)
                .data(data)
                .build();
    }


    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .status(TypeStatus.error)
                .message(message)
                .build();
    }
}

