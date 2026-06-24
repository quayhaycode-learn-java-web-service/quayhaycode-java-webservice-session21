package com.reptithcm.edu.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.reptithcm.edu.dto.UserLoginDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class LoginResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    @JsonProperty("user")
    private UserLoginDto user;
}
