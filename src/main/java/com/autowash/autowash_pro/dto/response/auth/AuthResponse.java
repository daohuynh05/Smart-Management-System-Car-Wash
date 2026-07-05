package com.autowash.autowash_pro.dto.response.auth;

import com.autowash.autowash_pro.enums.Tier;
import lombok.*;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private UUID id;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private String role;
    private Tier tier;
    private String fullName;
    private String phone;
}

