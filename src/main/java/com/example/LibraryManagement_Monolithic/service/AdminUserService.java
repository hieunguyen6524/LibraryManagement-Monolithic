package com.example.LibraryManagement_Monolithic.service;

import com.example.LibraryManagement_Monolithic.dto.mapper.UserMapper;
import com.example.LibraryManagement_Monolithic.dto.request.UserCreateRequest;
import com.example.LibraryManagement_Monolithic.dto.request.UserUpdateRequest;
import com.example.LibraryManagement_Monolithic.dto.response.PagedResponse;
import com.example.LibraryManagement_Monolithic.entity.Role;
import com.example.LibraryManagement_Monolithic.entity.User;
import com.example.LibraryManagement_Monolithic.exception.BadRequestException;
import com.example.LibraryManagement_Monolithic.exception.NotFoundException;
import com.example.LibraryManagement_Monolithic.repository.RoleRepository;
import com.example.LibraryManagement_Monolithic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PagedResponse<User> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> pageUsers = userRepo.findAll(pageable);

        return new PagedResponse<>(
                pageUsers.getContent(),
                pageUsers.getNumber(),
                pageUsers.getSize(),
                pageUsers.getTotalElements(),
                pageUsers.getTotalPages(),
                pageUsers.isLast()
        );
    }


    public User getUserByID(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new NotFoundException("Not found user with id: " + id));
    }

    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id " + id));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepo.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        userMapper.updateUserFromRequest(request, user);

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = request.getRoles().stream()
                    .map(roleName -> roleRepo.findByRoleName(roleName)
                            .orElseThrow(() -> new NotFoundException("Role not found: " + roleName)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        user.setUpdatedAt(LocalDateTime.now());

        return userRepo.save(user);
    }


    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    public User createUser(UserCreateRequest request) {

        if (userRepo.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepo.findByRoleName(roleName)
                        .orElseThrow(() -> new NotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .isActive(request.getIsActive())
                .isVerified(request.getIsVerified())
                .roles(roles)
                .build();

        return userRepo.save(user);
    }

}
