package com.buddkitv2.domain.user.service;

import com.buddkitv2.domain.user.dto.request.ChargeRequest;
import com.buddkitv2.domain.user.dto.request.ProfileUpdateRequest;
import com.buddkitv2.domain.user.dto.request.RegisterRequest;
import com.buddkitv2.domain.user.dto.response.ChargeResponse;
import com.buddkitv2.domain.user.dto.response.LikedClubResponse;
import com.buddkitv2.domain.user.dto.response.MyClubResponse;
import com.buddkitv2.domain.user.dto.response.MyPageResponse;
import com.buddkitv2.domain.user.dto.response.TransactionResponse;
import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
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
import com.buddkitv2.domain.wallet.repository.WalletTransactionRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadyRegisteredException;
import com.buddkitv2.global.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    @MockitoBean
    private S3Service s3Service;

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
    void 충전_성공_시_잔액이_증가한다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();
        Long initialBalance = result.getPoint(); // 100_000L

        String paymentKey = "test_pk_123";
        String orderId = "order_001";
        Long amount = 50_000L;

        given(tossPaymentClient.confirm(paymentKey, orderId, amount))
                .willReturn(new TossPaymentClient.TossConfirmResult(
                        paymentKey, orderId, "카드", amount,
                        LocalDateTime.of(2026, 1, 1, 10, 0)
                ));

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setPaymentKey(paymentKey);
        chargeRequest.setOrderId(orderId);
        chargeRequest.setAmount(amount);

        ChargeResponse response = userService.chargeWallet(userId, chargeRequest);

        assertThat(response.getBalance()).isEqualTo(initialBalance + amount);
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

    @Test
    void 가입한_모임이_없으면_빈_목록을_반환한다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        List<MyClubResponse> clubs = userService.getMyClubs(userId, null, 20);

        assertThat(clubs).isEmpty();
    }

    @Test
    void 관심_모임이_없으면_빈_목록을_반환한다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        List<LikedClubResponse> liked = userService.getLikedClubs(userId, null, 20);

        assertThat(liked).isEmpty();
    }

    @Test
    void FCM_토큰_등록_후_삭제_시_null이_된다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        userService.saveFcmToken(userId, "fcm-device-token-abc");
        User afterSave = userRepository.findById(userId).orElseThrow();
        assertThat(afterSave.getFcmToken()).isEqualTo("fcm-device-token-abc");

        userService.deleteFcmToken(userId);
        User afterDelete = userRepository.findById(userId).orElseThrow();
        assertThat(afterDelete.getFcmToken()).isNull();
    }

    @Test
    void 거래내역_목록_조회_시_최신순으로_반환된다() {
        UserService.RegisterResult result = userService.register(KAKAO_ID, request(), null);
        Long userId = result.getUserId();

        String paymentKey = "test_pk_456";
        String orderId = "order_002";
        Long amount = 30_000L;

        given(tossPaymentClient.confirm(paymentKey, orderId, amount))
                .willReturn(new TossPaymentClient.TossConfirmResult(
                        paymentKey, orderId, "카드", amount,
                        LocalDateTime.of(2026, 1, 2, 10, 0)
                ));

        ChargeRequest chargeRequest = new ChargeRequest();
        chargeRequest.setPaymentKey(paymentKey);
        chargeRequest.setOrderId(orderId);
        chargeRequest.setAmount(amount);
        userService.chargeWallet(userId, chargeRequest);

        List<TransactionResponse> transactions = userService.getTransactions(userId, null, 20);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(WalletTransactionType.CHARGE);
        assertThat(transactions.get(0).getBalance()).isEqualTo(amount);
    }
}
