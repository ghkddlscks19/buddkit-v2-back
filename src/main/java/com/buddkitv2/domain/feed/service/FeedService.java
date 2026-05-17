package com.buddkitv2.domain.feed.service;

import com.buddkitv2.domain.club.entity.UserClub;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.feed.dto.request.FeedCommentRequest;
import com.buddkitv2.domain.feed.dto.request.FeedCreateRequest;
import com.buddkitv2.domain.feed.dto.request.FeedUpdateRequest;
import com.buddkitv2.domain.feed.dto.response.FeedCommentResponse;
import com.buddkitv2.domain.feed.dto.response.FeedResponse;
import com.buddkitv2.domain.feed.entity.Feed;
import com.buddkitv2.domain.feed.entity.FeedComment;
import com.buddkitv2.domain.feed.entity.FeedImage;
import com.buddkitv2.domain.feed.entity.FeedLike;
import com.buddkitv2.domain.feed.entity.FeedSort;
import com.buddkitv2.domain.feed.repository.FeedCommentRepository;
import com.buddkitv2.domain.feed.repository.FeedImageRepository;
import com.buddkitv2.domain.feed.repository.FeedLikeRepository;
import com.buddkitv2.domain.feed.repository.FeedRepository;
import com.buddkitv2.global.exception.AlreadyLikedException;
import com.buddkitv2.global.exception.FeedAccessDeniedException;
import com.buddkitv2.global.exception.FeedCommentNotFoundException;
import com.buddkitv2.global.exception.FeedNotFoundException;
import com.buddkitv2.global.exception.NotLikedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserClubRepository userClubRepository;

    private UserClub requireMember(Long clubId, Long userId) {
        return userClubRepository.findByClub_IdAndUser_Id(clubId, userId)
                .orElseThrow(FeedAccessDeniedException::new);
    }

    private Feed findActiveFeed(Long clubId, Long feedId) {
        Feed feed = feedRepository.findActiveById(feedId)
                .orElseThrow(FeedNotFoundException::new);
        if (!feed.getClub().getId().equals(clubId)) {
            throw new FeedNotFoundException();
        }
        return feed;
    }

    private FeedResponse toResponse(Feed feed, Long userId) {
        List<String> imageUrls = feedImageRepository.findByFeed_Id(feed.getId())
                .stream().map(FeedImage::getImageUrl).toList();
        boolean isLiked = feedLikeRepository.existsByFeed_IdAndUser_Id(feed.getId(), userId);
        return new FeedResponse(
                feed.getId(), feed.getUser().getId(), feed.getUser().getNickname(),
                feed.getUser().getProfileImageUrl(), feed.getContent(),
                imageUrls, feed.getLikeCount(), isLiked, feed.getCreatedAt()
        );
    }

    private List<FeedResponse> toResponses(List<Feed> feeds, Long userId) {
        if (feeds.isEmpty()) {
            return List.of();
        }
        List<Long> feedIds = feeds.stream().map(Feed::getId).toList();
        Map<Long, List<String>> imageMap = feedImageRepository.findByFeed_IdIn(feedIds)
                .stream().collect(Collectors.groupingBy(
                        img -> img.getFeed().getId(),
                        Collectors.mapping(FeedImage::getImageUrl, Collectors.toList())));
        Set<Long> likedFeedIds = feedLikeRepository.findLikedFeedIds(userId, feedIds);
        return feeds.stream().map(feed -> new FeedResponse(
                feed.getId(), feed.getUser().getId(), feed.getUser().getNickname(),
                feed.getUser().getProfileImageUrl(), feed.getContent(),
                imageMap.getOrDefault(feed.getId(), List.of()),
                feed.getLikeCount(), likedFeedIds.contains(feed.getId()), feed.getCreatedAt()
        )).toList();
    }

    private FeedCommentResponse toCommentResponse(FeedComment comment) {
        return new FeedCommentResponse(
                comment.getId(), comment.getUser().getId(), comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(), comment.getContent(), comment.getCreatedAt()
        );
    }

    @Transactional
    public FeedResponse createFeed(Long userId, Long clubId, FeedCreateRequest req) {
        UserClub uc = requireMember(clubId, userId);
        Feed feed = feedRepository.save(Feed.create(req.getContent(), uc.getClub(), uc.getUser()));
        req.getImageUrls().forEach(url -> feedImageRepository.save(FeedImage.create(url, feed)));
        return toResponse(feed, userId);
    }

    @Transactional
    public FeedResponse updateFeed(Long userId, Long clubId, Long feedId, FeedUpdateRequest req) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        if (!feed.getUser().getId().equals(userId)) {
            throw new FeedAccessDeniedException();
        }
        feed.update(req.getContent());
        feedImageRepository.deleteByFeed_Id(feedId);
        req.getImageUrls().forEach(url -> feedImageRepository.save(FeedImage.create(url, feed)));
        return toResponse(feed, userId);
    }

    @Transactional
    public void deleteFeed(Long userId, Long clubId, Long feedId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        if (!feed.getUser().getId().equals(userId)) {
            throw new FeedAccessDeniedException();
        }
        feed.softDelete();
    }

    @Transactional(readOnly = true)
    public List<FeedResponse> getFeeds(Long userId, Long clubId, FeedSort sort, Long lastId, Integer page, int size) {
        requireMember(clubId, userId);
        List<Feed> feeds;
        if (sort == FeedSort.POPULAR) {
            feeds = feedRepository.findByClubIdOrderByPopular(clubId, PageRequest.of(page != null ? page : 0, size));
        } else {
            feeds = lastId == null
                    ? feedRepository.findByClubId(clubId, PageRequest.of(0, size))
                    : feedRepository.findByClubIdAndLastId(clubId, lastId, PageRequest.of(0, size));
        }
        return toResponses(feeds, userId);
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long userId, Long clubId, Long feedId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        return toResponse(feed, userId);
    }

    @Transactional
    public void likeFeed(Long userId, Long clubId, Long feedId) {
        UserClub uc = requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        if (feedLikeRepository.existsByFeed_IdAndUser_Id(feedId, userId)) {
            throw new AlreadyLikedException();
        }
        feedLikeRepository.save(FeedLike.create(feed, uc.getUser()));
        feed.incrementLike();
    }

    @Transactional
    public void unlikeFeed(Long userId, Long clubId, Long feedId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        FeedLike feedLike = feedLikeRepository.findByFeed_IdAndUser_Id(feedId, userId)
                .orElseThrow(NotLikedException::new);
        feedLikeRepository.delete(feedLike);
        feed.decrementLike();
    }

    @Transactional
    public FeedCommentResponse createComment(Long userId, Long clubId, Long feedId, FeedCommentRequest req) {
        UserClub uc = requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        FeedComment comment = feedCommentRepository.save(FeedComment.create(req.getContent(), feed, uc.getUser()));
        return toCommentResponse(comment);
    }

    @Transactional
    public FeedCommentResponse updateComment(Long userId, Long clubId, Long feedId, Long commentId, FeedCommentRequest req) {
        requireMember(clubId, userId);
        findActiveFeed(clubId, feedId);
        FeedComment comment = feedCommentRepository.findActiveById(commentId)
                .orElseThrow(FeedCommentNotFoundException::new);
        if (!comment.getFeed().getId().equals(feedId)) {
            throw new FeedCommentNotFoundException();
        }
        if (!comment.getUser().getId().equals(userId)) {
            throw new FeedAccessDeniedException();
        }
        comment.update(req.getContent());
        return toCommentResponse(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long clubId, Long feedId, Long commentId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        FeedComment comment = feedCommentRepository.findActiveById(commentId)
                .orElseThrow(FeedCommentNotFoundException::new);
        if (!comment.getFeed().getId().equals(feedId)) {
            throw new FeedCommentNotFoundException();
        }
        boolean isCommentAuthor = comment.getUser().getId().equals(userId);
        boolean isFeedAuthor = feed.getUser().getId().equals(userId);
        if (!isCommentAuthor && !isFeedAuthor) {
            throw new FeedAccessDeniedException();
        }
        comment.softDelete();
    }

    @Transactional(readOnly = true)
    public List<FeedCommentResponse> getComments(Long userId, Long clubId, Long feedId, Long lastId, int size) {
        requireMember(clubId, userId);
        findActiveFeed(clubId, feedId);
        List<FeedComment> comments = lastId == null
                ? feedCommentRepository.findByFeedId(feedId, PageRequest.of(0, size))
                : feedCommentRepository.findByFeedIdAndLastId(feedId, lastId, PageRequest.of(0, size));
        return comments.stream().map(this::toCommentResponse).toList();
    }
}
