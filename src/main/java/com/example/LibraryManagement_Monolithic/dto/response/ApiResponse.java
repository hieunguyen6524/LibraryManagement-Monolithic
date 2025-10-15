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
    private String errorCode;
    private T data;

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .status(TypeStatus.success)
                .message(message)
                .build();
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

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .status(TypeStatus.error)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode, T data) {
        return ApiResponse.<T>builder()
                .status(TypeStatus.error)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .build();
    }


    public enum TypeStatus {
        success,
        error
    }
}

