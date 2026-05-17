package com.buddkitv2.domain.feed.repository;

import com.buddkitv2.domain.feed.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

    List<FeedImage> findByFeed_Id(Long feedId);

    List<FeedImage> findByFeed_IdIn(List<Long> feedIds);

    void deleteByFeed_Id(Long feedId);
}
