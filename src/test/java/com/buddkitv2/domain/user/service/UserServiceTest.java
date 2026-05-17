package com.buddkitv2.domain.user.service;

import com.buddkitv2.domain.user.dto.request.ProfileUpdateRequest;
import com.buddkitv2.domain.user.dto.request.RegisterRequest;
import com.buddkitv2.domain.user.dto.response.MyPageResponse;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.entity.UserStatus;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.global.exception.AlreadyRegisteredException;
import com.buddkitv2.global.security.RefreshTokenService;
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

    @Autowired
    private RefreshTokenService refreshTokenService;

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
        assertThat(walletRepository.findByUserId(result.getUserId())).isPresent();
        assertThat(result.getPoint()).isEqualTo(100_000L);
    }

    @Test
    void 이미_가입된_회원은_재가입_시_예외를_던진다() {
        userService.register(KAKAO_ID, request(), null);

        assertThatThrownBy(() -> userService.register(KAKAO_ID, request(), null))
                .isInstanceOf(AlreadyRegisteredException.class)
                .hasMessage("이미 가입된 회원입니다.");
    }

    @Test
    void 프로필_수정_시_닉네임과_관심사가_변경된다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        Address newAddress = addressRepository.save(Address.of("부산광역시", "해운대구", 99001));
        interestRepository.save(Interest.of(InterestCategory.SPORTS, "운동/스포츠"));

        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setNickname("변경닉네임");
        updateRequest.setCity("부산광역시");
        updateRequest.setDistrict("해운대구");
        updateRequest.setInterests(List.of(InterestCategory.SPORTS));

        userService.updateProfile(userId, updateRequest, null);

        MyPageResponse profile = userService.getMyPage(userId);
        assertThat(profile.getNickname()).isEqualTo("변경닉네임");
        assertThat(profile.getCity()).isEqualTo("부산광역시");
        assertThat(profile.getInterestList()).containsExactly(InterestCategory.SPORTS);
    }

    @Test
    void 회원탈퇴_시_상태가_WITHDRAWN으로_변경된다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        userService.withdraw(userId);

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getFcmToken()).isNull();
    }
}
