package com.buddkitv2.domain.feed.service;

import com.buddkitv2.domain.club.dto.request.ClubCreateRequest;
import com.buddkitv2.domain.club.repository.UserClubRepository;
import com.buddkitv2.domain.club.service.ClubService;
import com.buddkitv2.domain.common.Address;
import com.buddkitv2.domain.common.AddressRepository;
import com.buddkitv2.domain.feed.dto.request.FeedCommentRequest;
import com.buddkitv2.domain.feed.dto.request.FeedCreateRequest;
import com.buddkitv2.domain.feed.dto.request.FeedUpdateRequest;
import com.buddkitv2.domain.feed.dto.response.FeedCommentResponse;
import com.buddkitv2.domain.feed.dto.response.FeedResponse;
import com.buddkitv2.domain.feed.entity.FeedSort;
import com.buddkitv2.domain.feed.repository.FeedCommentRepository;
import com.buddkitv2.domain.feed.repository.FeedImageRepository;
import com.buddkitv2.domain.feed.repository.FeedLikeRepository;
import com.buddkitv2.domain.feed.repository.FeedRepository;
import com.buddkitv2.domain.user.entity.Gender;
import com.buddkitv2.domain.user.entity.Interest;
import com.buddkitv2.domain.user.entity.InterestCategory;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.InterestRepository;
import com.buddkitv2.domain.user.repository.UserRepository;
import com.buddkitv2.global.config.S3Service;
import com.buddkitv2.global.config.TossPaymentClient;
import com.buddkitv2.global.exception.AlreadyLikedException;
import com.buddkitv2.global.exception.FeedAccessDeniedException;
import com.buddkitv2.global.exception.FeedCommentNotFoundException;
import com.buddkitv2.global.exception.FeedNotFoundException;
import com.buddkitv2.global.exception.NotLikedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FeedServiceTest {

    @Autowired FeedService feedService;
    @Autowired ClubService clubService;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired InterestRepository interestRepository;
    @Autowired UserClubRepository userClubRepository;
    @Autowired FeedRepository feedRepository;
    @Autowired FeedImageRepository feedImageRepository;
    @Autowired FeedLikeRepository feedLikeRepository;
    @Autowired FeedCommentRepository feedCommentRepository;

    @MockitoBean TossPaymentClient tossPaymentClient;
    @MockitoBean S3Service s3Service;

    private User author;
    private User otherMember;
    private User outsider;
    private Long clubId;

    @BeforeEach
    void setUp() {
        Address address = addressRepository.save(Address.of("서울특별시", "피드테스트구", 99001));
        interestRepository.save(Interest.of(InterestCategory.CULTURE, "문화"));

        author = userRepository.save(User.register(30001L, "작성자", LocalDate.of(1990, 1, 1), Gender.MALE, address, null));
        otherMember = userRepository.save(User.register(30002L, "다른멤버", LocalDate.of(1991, 2, 2), Gender.FEMALE, address, null));
        outsider = userRepository.save(User.register(30003L, "외부인", LocalDate.of(1992, 3, 3), Gender.MALE, address, null));

        ClubCreateRequest req = new ClubCreateRequest();
        req.setName("피드테스트모임");
        req.setUserLimit(10);
        req.setDescription("설명");
        req.setClubImage(null);
        req.setCity("서울특별시");
        req.setDistrict("피드테스트구");
        req.setInterestCategory(InterestCategory.CULTURE);
        clubId = clubService.createClub(author.getId(), req).getClubId();
        clubService.joinClub(otherMember.getId(), clubId);
    }

    private FeedCreateRequest createReq(String content, String... imageUrls) {
        FeedCreateRequest req = new FeedCreateRequest();
        req.setContent(content);
        req.setImageUrls(List.of(imageUrls));
        return req;
    }

    @Test
    void 모임_멤버는_피드를_생성할_수_있다() {
        FeedResponse res = feedService.createFeed(author.getId(), clubId,
                createReq("내용", "https://s3/img1.jpg", "https://s3/img2.jpg"));

        assertThat(res.getFeedId()).isNotNull();
        assertThat(res.getContent()).isEqualTo("내용");
        assertThat(res.getImageUrls()).hasSize(2);
        assertThat(res.getLikeCount()).isEqualTo(0L);
        assertThat(res.isLiked()).isFalse();
    }

    @Test
    void 모임_비멤버가_피드를_생성하면_예외를_던진다() {
        assertThatThrownBy(() ->
                feedService.createFeed(outsider.getId(), clubId, createReq("내용", "https://s3/img1.jpg")))
                .isInstanceOf(FeedAccessDeniedException.class);
    }

    @Test
    void 작성자는_피드를_수정할_수_있다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("원본", "https://s3/old.jpg"));
        Long feedId = created.getFeedId();

        FeedUpdateRequest req = new FeedUpdateRequest();
        req.setContent("수정됨");
        req.setImageUrls(List.of("https://s3/new1.jpg", "https://s3/new2.jpg"));
        FeedResponse updated = feedService.updateFeed(author.getId(), clubId, feedId, req);

        assertThat(updated.getContent()).isEqualTo("수정됨");
        assertThat(updated.getImageUrls()).containsExactly("https://s3/new1.jpg", "https://s3/new2.jpg");
        assertThat(feedImageRepository.findByFeed_Id(feedId)).hasSize(2);
    }

    @Test
    void 작성자가_아닌_멤버가_피드를_수정하면_예외를_던진다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("원본", "https://s3/old.jpg"));

        FeedUpdateRequest req = new FeedUpdateRequest();
        req.setContent("다른멤버가 수정");
        req.setImageUrls(List.of("https://s3/hack.jpg"));

        assertThatThrownBy(() ->
                feedService.updateFeed(otherMember.getId(), clubId, created.getFeedId(), req))
                .isInstanceOf(FeedAccessDeniedException.class);
    }

    @Test
    void 작성자는_피드를_삭제할_수_있다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("삭제될 피드", "https://s3/img.jpg"));
        Long feedId = created.getFeedId();

        feedService.deleteFeed(author.getId(), clubId, feedId);

        assertThat(feedRepository.findActiveById(feedId)).isEmpty();
    }

    @Test
    void 작성자가_아닌_멤버가_피드를_삭제하면_예외를_던진다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));

        assertThatThrownBy(() ->
                feedService.deleteFeed(otherMember.getId(), clubId, created.getFeedId()))
                .isInstanceOf(FeedAccessDeniedException.class);
    }

    @Test
    void 피드_목록을_최신순으로_조회할_수_있다() {
        feedService.createFeed(author.getId(), clubId, createReq("피드1", "https://s3/1.jpg"));
        feedService.createFeed(author.getId(), clubId, createReq("피드2", "https://s3/2.jpg"));
        feedService.createFeed(author.getId(), clubId, createReq("피드3", "https://s3/3.jpg"));

        List<FeedResponse> result = feedService.getFeeds(author.getId(), clubId, FeedSort.LATEST, null, null, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("피드3");
    }

    @Test
    void 피드_목록을_인기순으로_조회할_수_있다() {
        FeedResponse f1 = feedService.createFeed(author.getId(), clubId, createReq("인기없는피드", "https://s3/1.jpg"));
        FeedResponse f2 = feedService.createFeed(author.getId(), clubId, createReq("인기있는피드", "https://s3/2.jpg"));
        feedService.likeFeed(author.getId(), clubId, f2.getFeedId());
        feedService.likeFeed(otherMember.getId(), clubId, f2.getFeedId());

        List<FeedResponse> result = feedService.getFeeds(author.getId(), clubId, FeedSort.POPULAR, null, 0, 10);

        assertThat(result.get(0).getContent()).isEqualTo("인기있는피드");
        assertThat(result.get(0).getLikeCount()).isEqualTo(2L);
    }

    @Test
    void 피드_상세를_조회하면_이미지URL과_좋아요_여부가_포함된다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("상세조회피드", "https://s3/a.jpg", "https://s3/b.jpg"));
        feedService.likeFeed(otherMember.getId(), clubId, created.getFeedId());

        FeedResponse detail = feedService.getFeed(author.getId(), clubId, created.getFeedId());

        assertThat(detail.getImageUrls()).hasSize(2);
        assertThat(detail.getLikeCount()).isEqualTo(1L);
        assertThat(detail.isLiked()).isFalse();
    }

    @Test
    void 비멤버가_피드_상세를_조회하면_예외를_던진다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));

        assertThatThrownBy(() ->
                feedService.getFeed(outsider.getId(), clubId, created.getFeedId()))
                .isInstanceOf(FeedAccessDeniedException.class);
    }

    @Test
    void 멤버는_피드에_좋아요를_할_수_있다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));

        feedService.likeFeed(otherMember.getId(), clubId, created.getFeedId());

        FeedResponse detail = feedService.getFeed(otherMember.getId(), clubId, created.getFeedId());
        assertThat(detail.getLikeCount()).isEqualTo(1L);
        assertThat(detail.isLiked()).isTrue();
    }

    @Test
    void 이미_좋아요한_피드에_다시_좋아요하면_예외를_던진다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        feedService.likeFeed(otherMember.getId(), clubId, created.getFeedId());

        assertThatThrownBy(() ->
                feedService.likeFeed(otherMember.getId(), clubId, created.getFeedId()))
                .isInstanceOf(AlreadyLikedException.class);
    }

    @Test
    void 멤버는_좋아요를_취소할_수_있다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        feedService.likeFeed(otherMember.getId(), clubId, created.getFeedId());

        feedService.unlikeFeed(otherMember.getId(), clubId, created.getFeedId());

        FeedResponse detail = feedService.getFeed(otherMember.getId(), clubId, created.getFeedId());
        assertThat(detail.getLikeCount()).isEqualTo(0L);
        assertThat(detail.isLiked()).isFalse();
    }

    @Test
    void 좋아요하지_않은_피드를_취소하면_예외를_던진다() {
        FeedResponse created = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));

        assertThatThrownBy(() ->
                feedService.unlikeFeed(otherMember.getId(), clubId, created.getFeedId()))
                .isInstanceOf(NotLikedException.class);
    }

    @Test
    void 멤버는_댓글을_생성할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));

        FeedCommentRequest req = new FeedCommentRequest();
        req.setContent("댓글 내용");
        FeedCommentResponse comment = feedService.createComment(
                otherMember.getId(), clubId, feed.getFeedId(), req);

        assertThat(comment.getCommentId()).isNotNull();
        assertThat(comment.getContent()).isEqualTo("댓글 내용");
        assertThat(comment.getUserId()).isEqualTo(otherMember.getId());
    }

    @Test
    void 댓글_작성자는_댓글을_수정할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req = new FeedCommentRequest();
        req.setContent("원본 댓글");
        FeedCommentResponse comment = feedService.createComment(
                otherMember.getId(), clubId, feed.getFeedId(), req);

        FeedCommentRequest updReq = new FeedCommentRequest();
        updReq.setContent("수정된 댓글");
        FeedCommentResponse updated = feedService.updateComment(
                otherMember.getId(), clubId, feed.getFeedId(), comment.getCommentId(), updReq);

        assertThat(updated.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    void 댓글_작성자가_아닌_멤버가_댓글을_수정하면_예외를_던진다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req = new FeedCommentRequest();
        req.setContent("타인의 댓글");
        FeedCommentResponse comment = feedService.createComment(
                otherMember.getId(), clubId, feed.getFeedId(), req);

        FeedCommentRequest updReq = new FeedCommentRequest();
        updReq.setContent("무단 수정");

        assertThatThrownBy(() ->
                feedService.updateComment(author.getId(), clubId, feed.getFeedId(), comment.getCommentId(), updReq))
                .isInstanceOf(FeedAccessDeniedException.class);
    }

    @Test
    void 댓글_작성자는_댓글을_삭제할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req = new FeedCommentRequest();
        req.setContent("삭제될 댓글");
        FeedCommentResponse comment = feedService.createComment(
                otherMember.getId(), clubId, feed.getFeedId(), req);

        feedService.deleteComment(otherMember.getId(), clubId, feed.getFeedId(), comment.getCommentId());

        assertThat(feedCommentRepository.findActiveById(comment.getCommentId())).isEmpty();
    }

    @Test
    void 피드_작성자도_타인의_댓글을_삭제할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req = new FeedCommentRequest();
        req.setContent("타인의 댓글");
        FeedCommentResponse comment = feedService.createComment(
                otherMember.getId(), clubId, feed.getFeedId(), req);

        feedService.deleteComment(author.getId(), clubId, feed.getFeedId(), comment.getCommentId());

        assertThat(feedCommentRepository.findActiveById(comment.getCommentId())).isEmpty();
    }

    @Test
    void 댓글_목록을_오름차순_cursor로_조회할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req1 = new FeedCommentRequest(); req1.setContent("첫번째");
        FeedCommentRequest req2 = new FeedCommentRequest(); req2.setContent("두번째");
        FeedCommentRequest req3 = new FeedCommentRequest(); req3.setContent("세번째");
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req1);
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req2);
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req3);

        List<FeedCommentResponse> result = feedService.getComments(
                author.getId(), clubId, feed.getFeedId(), null, 10);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("첫번째");
        assertThat(result.get(2).getContent()).isEqualTo("세번째");
    }

    @Test
    void 피드_목록_두번째_페이지를_cursor로_조회할_수_있다() {
        feedService.createFeed(author.getId(), clubId, createReq("피드1", "https://s3/1.jpg"));
        feedService.createFeed(author.getId(), clubId, createReq("피드2", "https://s3/2.jpg"));
        feedService.createFeed(author.getId(), clubId, createReq("피드3", "https://s3/3.jpg"));

        List<FeedResponse> firstPage = feedService.getFeeds(author.getId(), clubId, FeedSort.LATEST, null, null, 2);
        Long lastId = firstPage.get(firstPage.size() - 1).getFeedId();

        List<FeedResponse> secondPage = feedService.getFeeds(author.getId(), clubId, FeedSort.LATEST, lastId, null, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).getContent()).isEqualTo("피드3");
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).getContent()).isEqualTo("피드1");
    }

    @Test
    void 댓글_목록_두번째_페이지를_cursor로_조회할_수_있다() {
        FeedResponse feed = feedService.createFeed(author.getId(), clubId,
                createReq("피드", "https://s3/img.jpg"));
        FeedCommentRequest req1 = new FeedCommentRequest(); req1.setContent("댓글1");
        FeedCommentRequest req2 = new FeedCommentRequest(); req2.setContent("댓글2");
        FeedCommentRequest req3 = new FeedCommentRequest(); req3.setContent("댓글3");
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req1);
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req2);
        feedService.createComment(author.getId(), clubId, feed.getFeedId(), req3);

        List<FeedCommentResponse> firstPage = feedService.getComments(
                author.getId(), clubId, feed.getFeedId(), null, 2);
        Long lastId = firstPage.get(firstPage.size() - 1).getCommentId();

        List<FeedCommentResponse> secondPage = feedService.getComments(
                author.getId(), clubId, feed.getFeedId(), lastId, 2);

        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0).getContent()).isEqualTo("댓글1");
        assertThat(secondPage).hasSize(1);
        assertThat(secondPage.get(0).getContent()).isEqualTo("댓글3");
    }
}
