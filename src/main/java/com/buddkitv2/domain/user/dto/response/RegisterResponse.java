package com.buddkitv2.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private String accessToken;
    private String refreshToken;
    private Long point;
}
