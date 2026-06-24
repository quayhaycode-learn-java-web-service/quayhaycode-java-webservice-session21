package com.reptithcm.edu.controller.auth;

import com.reptithcm.edu.dto.request.LoginRequest;
import com.reptithcm.edu.dto.request.LogoutRequest;
import com.reptithcm.edu.dto.request.RefreshTokenRequest;
import com.reptithcm.edu.dto.request.RegisterRequest;
import com.reptithcm.edu.dto.response.LoginResponse;
import com.reptithcm.edu.dto.response.RefreshTokenResponse;
import com.reptithcm.edu.dto.response.RegisterResponse;
import com.reptithcm.edu.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.handleRegister(registerRequest);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/api/users/{id}")
                .buildAndExpand(registerResponse.getUser().getId()).toUri();

        return ResponseEntity.created(location).body(registerResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse loginResponse = authService.handleLogin(request);
        return ResponseEntity.ok(loginResponse);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = authService.handleRefreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("isAuthenticated()")
    // Delete the refresh token from the database to terminate the user session
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        authService.handleLogout(request);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{userId}")
    public ResponseEntity<String> checkStatus(@PathVariable Long userId) {
        boolean online = authService.isUserLoggedIn(userId);
        return ResponseEntity.ok(online ? "Active user" : "The user has no login session!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/revoke/{userId}")
    public ResponseEntity<String> forceLogout(@PathVariable Long userId) {
        authService.handleRevokeToken(userId);
        return ResponseEntity.ok("User ID's access has been revoked: " + userId);
    }

}
