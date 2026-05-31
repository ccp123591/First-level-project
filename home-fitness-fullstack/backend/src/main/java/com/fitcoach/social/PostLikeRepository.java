package com.fitcoach.social;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    long deleteByPostIdAndUserId(Long postId, Long userId);
    List<PostLike> findByUserIdAndPostIdIn(Long userId, Collection<Long> postIds);
}
