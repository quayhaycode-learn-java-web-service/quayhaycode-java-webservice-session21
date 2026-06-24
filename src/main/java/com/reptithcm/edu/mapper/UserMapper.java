package com.reptithcm.edu.mapper;

import com.reptithcm.edu.dto.UserLoginDto;
import com.reptithcm.edu.dto.UserRegisterDto;
import com.reptithcm.edu.dto.request.RegisterRequest;
import com.reptithcm.edu.dto.response.RegisterResponse;
import com.reptithcm.edu.entity.user.User;
import com.reptithcm.edu.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public User toUserRegister(RegisterRequest registerRequest, String encodedPassword) {
        if (registerRequest == null) {
            return null;
        }

        return User.builder()
                .username(registerRequest.getUsername())
                .password(encodedPassword) // Receive encoded password
                .fullName(registerRequest.getFullName())
                .email(registerRequest.getEmail())
                .phone(registerRequest.getPhone())
                .address(registerRequest.getAddress())
                .enabled(true) // Default when register
                .userRoles(new HashSet<>())
                .build();
    }

    public RegisterResponse toRegisterResponse(User user){

        Set<String> roleNames = (user.getUserRoles() == null)
                ? Collections.emptySet()
                : user.getUserRoles().stream()
                .filter(ur -> ur.getRole() != null) // Additional protection if the role is null
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());

        UserRegisterDto userRegisterDto = UserRegisterDto.builder()
                .id(user.getId()).username(user.getUsername()).fullName(user.getFullName()).email(user.getEmail())
                .roles(roleNames).createdAt(LocalDateTime.now()).build();

        return RegisterResponse.builder()
                .message("Success")
                .user(userRegisterDto)
                .build();
    }

    public UserLoginDto toUserLogin(Object principal) {
        if (principal instanceof UserDetailsImpl userDetails){
            return UserLoginDto.builder()
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .fullName(userDetails.getFullName())
                    .email(userDetails.getEmail())
                    .roles(
                            userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet())
                    )
                    .lastLogin(LocalDateTime.now())
                    .build();
        }
        return null;
    }
}

