package com.reptithcm.edu.service.auth;

import com.reptithcm.edu.dto.UserLoginDto;
import com.reptithcm.edu.dto.request.LoginRequest;
import com.reptithcm.edu.dto.request.LogoutRequest;
import com.reptithcm.edu.dto.request.RefreshTokenRequest;
import com.reptithcm.edu.dto.request.RegisterRequest;
import com.reptithcm.edu.dto.response.LoginResponse;
import com.reptithcm.edu.dto.response.RefreshTokenResponse;
import com.reptithcm.edu.dto.response.RegisterResponse;
import com.reptithcm.edu.entity.user.RefreshToken;
import com.reptithcm.edu.entity.user.Role;
import com.reptithcm.edu.entity.user.User;
import com.reptithcm.edu.entity.user.UserRole;
import com.reptithcm.edu.mapper.UserMapper;
import com.reptithcm.edu.repository.user.RefreshTokenRepository;
import com.reptithcm.edu.repository.user.RoleRepository;
import com.reptithcm.edu.repository.user.UserRepository;
import com.reptithcm.edu.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;


    @Value("${app.jwt.refresh-expires-in-mili-seconds}")
    private long refreshTokenExpirationMs;
    @Value("${app.jwt.access-expires-in-mili-seconds}")
    private long accessTokenExpirationMs;

    // register
    @Transactional
    public RegisterResponse handleRegister(RegisterRequest registerRequest){
        // validate
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("username is exists");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("email is exists");
        }

        // hashing password
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());

        // create user
        User user = userMapper.toUserRegister(registerRequest, encodedPassword);

        // get default role
        Role defaultRole = roleRepository.findRoleByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_USER not found."));

        // create userrole + addto user
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(defaultRole)
                .grantedAt(Instant.now())
                .grantedBy("SYSTEM")
                .build();

        user.getUserRoles().add(userRole);

        // save user to database
        User savedUser = userRepository.save(user);

        return userMapper.toRegisterResponse(user);
    }

    public LoginResponse handleLogin(LoginRequest request){
        // 1. Perform authentication through AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        // 2. Save the credentials to the Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Generate a Access Token from the authentication object
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // 4. Transfer principal and map information to DTO
        Object principal = authentication.getPrincipal();
        UserLoginDto userLoginDto = userMapper.toUserLogin(principal);
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // 5. Create Refresh Token and save to DB
        // Should remove old Refresh Token of this User to ensure (1 user - 1 session)
        // 5. Returns LoginResponse along with the Access Token

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(refreshToken);

        return LoginResponse.builder()
                .message("Login successfully!")
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs)
                .user(userLoginDto)
                .build();
    }

    @Transactional
    public RefreshTokenResponse handleRefreshToken(RefreshTokenRequest request){
        String requestRefreshToken = request.getRefreshToken();
        // 1. Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findRefreshTokenByRefreshToken(requestRefreshToken).orElseThrow(() -> new RuntimeException("Refreshtoken not found"));

        // 2. Check refresh token expired or not
        if (refreshToken.getExpiryDate().isBefore(Instant.now())){
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }

        // 3. Get User info from valid refresh token
        User user = refreshToken.getUser();

        // 4. Generate new Access Token from username
        String accessToken = tokenProvider.generateAccessTokenFromUsername(user.getUsername());

        // 5. Return response DTO
        return new RefreshTokenResponse(accessToken, requestRefreshToken, "Bearer");
    }

    public void handleLogout(LogoutRequest logoutRequest) {
        String refreshToken = logoutRequest.getRefreshToken();
        refreshTokenRepository.findRefreshTokenByRefreshToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

    public boolean isUserLoggedIn(Long userId) {
        return refreshTokenRepository.findByUserId(userId).isPresent();
    }

    public void handleRevokeToken(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("The user does not exist!");
        }
        refreshTokenRepository.deleteByUserId(userId);
    }
}
