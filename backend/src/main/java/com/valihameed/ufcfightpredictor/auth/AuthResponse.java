package com.valihameed.ufcfightpredictor.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private long expiresInSeconds;
}
