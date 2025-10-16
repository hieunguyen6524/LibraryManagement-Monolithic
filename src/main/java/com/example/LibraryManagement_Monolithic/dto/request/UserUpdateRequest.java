package com.example.LibraryManagement_Monolithic.dto.request;


import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserUpdateRequest {
    private String email;
    private String name;
    private Boolean isActive;
    private Boolean isVerified;
    private Set<String> roles; // roleName (USER, ADMIN, ...)
}