package com.buddkitv2.api.user;

import com.buddkitv2.domain.user.Gender;
import com.buddkitv2.domain.user.InterestCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,10}$", message = "닉네임은 2~10자의 영문, 한글, 숫자만 가능합니다.")
    private String nickname;

    @NotNull
    private LocalDate birth;

    @NotNull
    private Gender gender;

    @NotBlank
    private String city;

    @NotBlank
    private String district;

    @NotNull
    @Size(min = 1, max = 5, message = "관심사는 1개 이상 5개 이하로 선택해주세요.")
    private List<InterestCategory> interests;
}
