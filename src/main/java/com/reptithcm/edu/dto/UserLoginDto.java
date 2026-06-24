package com.reptithcm.edu.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserLoginDto {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Set<String> roles;
    private LocalDateTime lastLogin;
}
