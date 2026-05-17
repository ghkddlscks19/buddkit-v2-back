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
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.exception.AlreadyLikedException;
import com.buddkitv2.global.exception.FeedAccessDeniedException;
import com.buddkitv2.global.exception.FeedCommentNotFoundException;
import com.buddkitv2.global.exception.FeedNotFoundException;
import com.buddkitv2.global.exception.NotLikedException;
import com.buddkitv2.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final UserClubRepository userClubRepository;
    private final UserRepository userRepository;

    // ── 헬퍼 ──────────────────────────────────────────────

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

    private FeedCommentResponse toCommentResponse(FeedComment comment) {
        return new FeedCommentResponse(
                comment.getId(), comment.getUser().getId(), comment.getUser().getNickname(),
                comment.getUser().getProfileImageUrl(), comment.getContent(), comment.getCreatedAt()
        );
    }

    // ── 피드 CRUD ─────────────────────────────────────────

    @Transactional
    public FeedResponse createFeed(Long userId, Long clubId, FeedCreateRequest req) {
        UserClub uc = requireMember(clubId, userId);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Feed feed = feedRepository.save(Feed.create(req.getContent(), uc.getClub(), user));
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
        return feeds.stream().map(f -> toResponse(f, userId)).toList();
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long userId, Long clubId, Long feedId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        return toResponse(feed, userId);
    }

    // ── 좋아요 ────────────────────────────────────────────

    @Transactional
    public void likeFeed(Long userId, Long clubId, Long feedId) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        if (feedLikeRepository.existsByFeed_IdAndUser_Id(feedId, userId)) {
            throw new AlreadyLikedException();
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        feedLikeRepository.save(FeedLike.create(feed, user));
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

    // ── 댓글 ──────────────────────────────────────────────

    @Transactional
    public FeedCommentResponse createComment(Long userId, Long clubId, Long feedId, FeedCommentRequest req) {
        requireMember(clubId, userId);
        Feed feed = findActiveFeed(clubId, feedId);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        FeedComment comment = feedCommentRepository.save(FeedComment.create(req.getContent(), feed, user));
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
