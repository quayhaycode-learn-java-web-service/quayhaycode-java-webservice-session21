package com.reptithcm.edu.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.reptithcm.edu.dto.UserRegisterDto;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RegisterResponse {
    private String message;

    @JsonProperty("user")
    private UserRegisterDto user;
}
