package com.buddkitv2.api.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private String accessToken;
    private String refreshToken;
    private Long point;
}
