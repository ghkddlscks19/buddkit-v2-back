package com.buddkitv2.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ChargeRequest {

    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @NotNull
    @Min(1)
    private Long amount;
}
