package com.buddkitv2.domain.user.service;

import com.buddkitv2.domain.user.dto.request.ChargeRequest;
import com.buddkitv2.domain.user.dto.request.ProfileUpdateRequest;
import com.buddkitv2.domain.user.dto.request.RegisterRequest;
import com.buddkitv2.domain.user.dto.response.ChargeResponse;
import com.buddkitv2.domain.user.dto.response.MyPageResponse;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.entity.UserInterest;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserInterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.domain.club.entity.ClubLike;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.repository.ClubLikeRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.settlement.entity.UserSettlement;
import com.buddkitv2.domain.settlement.repository.UserSettlementRepository;
import com.buddkitv2.domain.user.dto.response.LikedClubResponse;
import com.buddkitv2.domain.user.dto.response.MyClubResponse;
import com.buddkitv2.domain.user.dto.response.SettlementHistoryResponse;
import com.buddkitv2.domain.user.dto.response.TransactionResponse;
import com.buddkitv2.domain.wallet.entity.Payment;
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.entity.WalletTransaction;
import com.buddkitv2.domain.wallet.entity.WalletTransactionType;
import com.buddkitv2.domain.wallet.repository.PaymentRepository;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadyRegisteredException;
import com.buddkitv2.global.security.RefreshTokenService;
import com.buddkitv2.global.exception.InvalidAddressException;
import com.buddkitv2.global.exception.InvalidInterestException;
import com.buddkitv2.global.exception.UserNotFoundException;
import com.buddkitv2.global.exception.WalletNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final InterestRepository interestRepository;
    private final UserInterestRepository userInterestRepository;
    private final WalletRepository walletRepository;
    private final S3Service s3Service;
    private final RefreshTokenService refreshTokenService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final UserSettlementRepository userSettlementRepository;
    private final UserClubRepository userClubRepository;
    private final ClubLikeRepository clubLikeRepository;

    private static final long SIGNUP_BONUS = 100_000L;

    @Transactional(readOnly = true)
    public Optional<User> findByKakaoId(Long kakaoId) {
        return userRepository.findByKakaoId(kakaoId);
    }

    @Transactional
    public RegisterResult register(Long kakaoId, RegisterRequest request, MultipartFile profileImage) {
        if (userRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new AlreadyRegisteredException();
        }

        Address address = addressRepository.findByCityAndDistrict(request.getCity(), request.getDistrict())
                .orElseThrow(InvalidAddressException::new);

        List<Interest> interests = interestRepository.findByCategoryIn(request.getInterests());
        if (interests.size() != request.getInterests().size()) {
            throw new InvalidInterestException();
        }

        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = s3Service.upload(profileImage, "profiles");
        }

        User user = User.register(kakaoId, request.getNickname(), request.getBirth(),
                request.getGender(), address, profileImageUrl);
        userRepository.save(user);

        interests.forEach(interest -> userInterestRepository.save(UserInterest.create(user, interest)));

        Wallet wallet = Wallet.createWithBonus(user, SIGNUP_BONUS);
        walletRepository.save(wallet);

        return new RegisterResult(user.getId(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        List<InterestCategory> interests = userInterestRepository.findByUserIdWithInterest(userId).stream()
                .map(ui -> ui.getInterest().getCategory())
                .toList();

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        Address address = user.getAddress();
        return new MyPageResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                address != null ? address.getCity() : null,
                address != null ? address.getDistrict() : null,
                user.getBirth(),
                user.getGender(),
                interests,
                wallet.getBalance()
        );
    }

    @Transactional
    public MyPageResponse updateProfile(Long userId, ProfileUpdateRequest request, MultipartFile profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Address address = addressRepository.findByCityAndDistrict(request.getCity(), request.getDistrict())
                .orElseThrow(InvalidAddressException::new);

        List<Interest> interests = interestRepository.findByCategoryIn(request.getInterests());
        if (interests.size() != request.getInterests().size()) {
            throw new InvalidInterestException();
        }

        String profileImageUrl = user.getProfileImageUrl();
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = s3Service.upload(profileImage, "profiles");
        }

        user.updateProfile(request.getNickname(), address, profileImageUrl);

        userInterestRepository.deleteAllByUserId(userId);
        interests.forEach(interest -> userInterestRepository.save(UserInterest.create(user, interest)));

        return getMyPage(userId);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.withdraw();
        user.updateFcmToken(null);
        refreshTokenService.delete(userId);
    }

    @Transactional
    public ChargeResponse chargeWallet(Long userId, ChargeRequest request) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);

        TossPaymentClient.TossConfirmResult confirmed =
                tossPaymentClient.confirm(request.getPaymentKey(), request.getOrderId(), request.getAmount());

        WalletTransaction transaction = WalletTransaction.create(
                wallet, wallet, WalletTransactionType.CHARGE, confirmed.totalAmount()
        );
        walletTransactionRepository.save(transaction);

        Payment payment = Payment.create(
                transaction,
                confirmed.paymentKey(),
                confirmed.orderId(),
                confirmed.method(),
                confirmed.totalAmount(),
                confirmed.approvedAt()
        );
        paymentRepository.save(payment);

        wallet.charge(confirmed.totalAmount());

        return new ChargeResponse(wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long userId, Long lastId, int size) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(WalletNotFoundException::new);
        Pageable pageable = PageRequest.of(0, size);

        List<WalletTransaction> transactions = lastId == null
                ? walletTransactionRepository.findByWallet_IdOrderByIdDesc(wallet.getId(), pageable)
                : walletTransactionRepository.findByWallet_IdAndIdLessThanOrderByIdDesc(wallet.getId(), lastId, pageable);

        return transactions.stream()
                .map(t -> new TransactionResponse(t.getId(), t.getType(), t.getBalance(), t.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SettlementHistoryResponse> getSettlements(Long userId, Long lastId, int size) {
        Pageable pageable = PageRequest.of(0, size);

        List<UserSettlement> settlements = lastId == null
                ? userSettlementRepository.findByUserIdWithSettlement(userId, pageable)
                : userSettlementRepository.findByUserIdAndLastIdWithSettlement(userId, lastId, pageable);

        return settlements.stream()
                .map(us -> new SettlementHistoryResponse(
                        us.getId(),
                        us.getStatus(),
                        us.getType(),
                        us.getSettlement().getSum(),
                        us.getCompletedTime(),
                        us.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MyClubResponse> getMyClubs(Long userId, Long lastId, int size) {
        Pageable pageable = PageRequest.of(0, size);

        List<UserClub> userClubs = lastId == null
                ? userClubRepository.findByUserIdWithClub(userId, pageable)
                : userClubRepository.findByUserIdAndLastIdWithClub(userId, lastId, pageable);

        return userClubs.stream().map(uc -> {
            var club = uc.getClub();
            var addr = club.getAddress();
            var interest = club.getInterest();
            return new MyClubResponse(
                    club.getId(), club.getName(), club.getClubImage(),
                    interest.getCategory(), interest.getName(),
                    addr.getCity(), addr.getDistrict(),
                    club.getMemberCount(), uc.getRole().name()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LikedClubResponse> getLikedClubs(Long userId, Long lastId, int size) {
        Pageable pageable = PageRequest.of(0, size);

        List<ClubLike> clubLikes = lastId == null
                ? clubLikeRepository.findByUserIdWithClub(userId, pageable)
                : clubLikeRepository.findByUserIdAndLastIdWithClub(userId, lastId, pageable);

        return clubLikes.stream().map(cl -> {
            var club = cl.getClub();
            var addr = club.getAddress();
            var interest = club.getInterest();
            return new LikedClubResponse(
                    club.getId(), club.getName(), club.getClubImage(),
                    interest.getCategory(), interest.getName(),
                    addr.getCity(), addr.getDistrict(),
                    club.getMemberCount()
            );
        }).toList();
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResult {
        private Long userId;
        private Long point;
    }
}
