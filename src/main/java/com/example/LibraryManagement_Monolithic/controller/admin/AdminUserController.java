package com.example.LibraryManagement_Monolithic.controller.admin;

import com.example.LibraryManagement_Monolithic.dto.request.UserCreateRequest;
import com.example.LibraryManagement_Monolithic.dto.request.UserUpdateRequest;
import com.example.LibraryManagement_Monolithic.dto.response.ApiResponse;
import com.example.LibraryManagement_Monolithic.dto.response.PagedResponse;
import com.example.LibraryManagement_Monolithic.entity.User;
import com.example.LibraryManagement_Monolithic.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PagedResponse<User> users = adminUserService.getAllUsers(page, size, sortBy, sortDir);

        return ResponseEntity.ok(ApiResponse.success("Get all users successfully", users));
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        User user = adminUserService.getUserByID(id);

        return ResponseEntity.status(200).body((ApiResponse.success("Get success user", user)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        User updated = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserCreateRequest request) {
        User users = adminUserService.createUser(request);
        return ResponseEntity.status(200).body(ApiResponse.success("Get all users", users));
    }

}
