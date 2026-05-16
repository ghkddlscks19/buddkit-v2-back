package com.buddkitv2.application.user;

import com.buddkitv2.api.user.RegisterRequest;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.*;
import com.buddkitv2.domain.wallet.Wallet;
import com.buddkitv2.domain.wallet.WalletRepository;
import com.buddkitv2.infra.s3.S3Service;
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

    private static final long SIGNUP_BONUS = 100_000L;

    @Transactional(readOnly = true)
    public Optional<User> findByKakaoId(Long kakaoId) {
        return userRepository.findByKakaoId(kakaoId);
    }

    @Transactional
    public RegisterResult register(Long kakaoId, RegisterRequest request, MultipartFile profileImage) {
        if (userRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }

        Address address = addressRepository.findByCityAndDistrict(request.getCity(), request.getDistrict())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 지역입니다."));

        List<Interest> interests = interestRepository.findByCategoryIn(request.getInterests());
        if (interests.size() != request.getInterests().size()) {
            throw new IllegalArgumentException("유효하지 않은 관심사가 포함되어 있습니다.");
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

    @Getter
    @AllArgsConstructor
    public static class RegisterResult {
        private Long userId;
        private Long point;
    }
}
