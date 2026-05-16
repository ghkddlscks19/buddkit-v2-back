package com.buddkitv2.domain.user.service;

import com.buddkitv2.domain.user.dto.request.RegisterRequest;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private WalletRepository walletRepository;

    private static final Long KAKAO_ID = 99999L;

    private Address address;
    private List<InterestCategory> categories;

    @BeforeEach
    void setUp() {
        address = addressRepository.save(Address.of("서울특별시", "테스트구", 99000));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));
        categories = List.of(InterestCategory.CULTURE);
    }

    private RegisterRequest request() {
        return new RegisterRequest("테스트유저", LocalDate.of(2000, 1, 1),
                Gender.FEMALE, address.getCity(), address.getDistrict(), categories);
    }

    @Test
    void 신규_회원_가입_시_User_Wallet_UserInterest가_생성된다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);

        assertThat(userRepository.findByKakaoId(KAKAO_ID)).isPresent();
        assertThat(walletRepository.findById(result.getUserId())).isNotNull();
        assertThat(result.getPoint()).isEqualTo(100_000L);
    }

    @Test
    void 이미_가입된_회원은_재가입_시_예외를_던진다() {
        userService.register(KAKAO_ID, request(), null);

        assertThatThrownBy(() -> userService.register(KAKAO_ID, request(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 가입된 회원입니다.");
    }
}
