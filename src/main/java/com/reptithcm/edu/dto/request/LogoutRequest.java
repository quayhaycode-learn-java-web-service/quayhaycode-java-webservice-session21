package com.reptithcm.edu.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LogoutRequest {
    @NotBlank
    private String refreshToken;
}
