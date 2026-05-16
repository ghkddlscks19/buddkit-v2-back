package com.buddkitv2.domain.user.service;

import com.buddkitv2.domain.user.dto.request.RegisterRequest;
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
import com.buddkitv2.domain.wallet.entity.Wallet;
import com.buddkitv2.domain.wallet.repository.WalletRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.exception.AlreadyRegisteredException;
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
                interests,
                wallet.getBalance()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class RegisterResult {
        private Long userId;
        private Long point;
    }
}
