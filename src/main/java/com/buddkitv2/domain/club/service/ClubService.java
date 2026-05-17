package com.buddkitv2.domain.club.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.dto.request.ClubUpdateRequest;
import com.buddkitv2.domain.club.dto.response.ClubDetailResponse;
import com.buddkitv2.domain.club.entity.Club;
import com.buddkitv2.domain.club.entity.ClubLike;
import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.entity.UserClubRole;
import com.buddkitv2.domain.club.repository.ClubLikeRepository;
import com.buddkitv2.domain.club.repository.ClubRepository;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final InterestRepository interestRepository;
    private final UserClubRepository userClubRepository;
    private final ClubLikeRepository clubLikeRepository;

    private Interest findInterest(InterestCategory category) {
        return interestRepository.findByCategory(category)
                .orElseThrow(InvalidInterestException::new);
    }

    private Address findAddress(String city, String district) {
        return addressRepository.findByCityAndDistrict(city, district)
                .orElseThrow(InvalidAddressException::new);
    }

    private ClubDetailResponse buildDetailResponse(Club club, Long userId) {
        boolean isLiked = clubLikeRepository.existsByClub_IdAndUser_Id(club.getId(), userId);
        Optional<UserClub> userClub = userClubRepository.findByClub_IdAndUser_Id(club.getId(), userId);
        boolean isMember = userClub.isPresent();
        String myRole = userClub.map(uc -> uc.getRole().name()).orElse(null);
        Address address = club.getAddress();
        Interest interest = club.getInterest();
        return new ClubDetailResponse(
                club.getId(), club.getName(), club.getDescription(), club.getClubImage(),
                club.getUserLimit(), club.getMemberCount(),
                address.getCity(), address.getDistrict(),
                interest.getCategory(), interest.getName(),
                isLiked, isMember, myRole
        );
    }

    @Transactional
    public ClubDetailResponse createClub(Long userId, ClubCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Address address = findAddress(request.getCity(), request.getDistrict());
        Interest interest = findInterest(request.getInterestCategory());
        Club club = Club.create(request.getName(), request.getUserLimit(), request.getDescription(),
                request.getClubImage(), address, interest);
        clubRepository.save(club);
        userClubRepository.save(UserClub.create(club, user, UserClubRole.LEADER));
        return buildDetailResponse(club, userId);
    }

    @Transactional
    public ClubDetailResponse updateClub(Long userId, Long clubId, ClubUpdateRequest request) {
        Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
        userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .filter(uc -> uc.getRole() == UserClubRole.LEADER)
                .orElseThrow(ClubAccessDeniedException::new);
        Address address = findAddress(request.getCity(), request.getDistrict());
        Interest interest = findInterest(request.getInterestCategory());
        club.update(request.getName(), request.getUserLimit(), request.getDescription(),
                request.getClubImage(), address, interest);
        return buildDetailResponse(club, userId);
    }

    @Transactional(readOnly = true)
    public ClubDetailResponse getClub(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
        return buildDetailResponse(club, userId);
    }

    @Transactional
    public void joinClub(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
        if (userClubRepository.existsByClub_IdAndUser_Id(clubId, userId)) {
            throw new AlreadyJoinedClubException();
        }
        if (club.getMemberCount() >= club.getUserLimit()) {
            throw new ClubFullException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        userClubRepository.save(UserClub.create(club, user, UserClubRole.MEMBER));
        club.incrementMemberCount();
    }

    @Transactional
    public void leaveClub(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
        UserClub userClub = userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(NotJoinedClubException::new);
        if (userClub.getRole() == UserClubRole.LEADER) {
            throw new ClubLeaderCannotLeaveException();
        }
        userClubRepository.delete(userClub);
        club.decrementMemberCount();
    }

    @Transactional
    public void likeClub(Long userId, Long clubId) {
        Club club = clubRepository.findById(clubId).orElseThrow(ClubNotFoundException::new);
        if (clubLikeRepository.existsByClub_IdAndUser_Id(clubId, userId)) {
            throw new AlreadyLikedClubException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        clubLikeRepository.save(ClubLike.create(user, club));
    }

    @Transactional
    public void unlikeClub(Long userId, Long clubId) {
        ClubLike clubLike = clubLikeRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(ClubLikeNotFoundException::new);
        clubLikeRepository.delete(clubLike);
    }
}
