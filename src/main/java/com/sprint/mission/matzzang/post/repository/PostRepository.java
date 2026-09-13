package com.sprint.mission.matzzang.post.repository;

import com.sprint.mission.matzzang.post.entity.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE :cursor IS NULL OR p.id > :cursor ORDER BY p.id ASC")
    List<Post> findAllByCursor(@Param("cursor") Long cursor, Pageable pageable);
}
