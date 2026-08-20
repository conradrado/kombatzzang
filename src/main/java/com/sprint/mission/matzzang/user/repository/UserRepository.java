package com.sprint.mission.matzzang.user.repository;

import com.sprint.mission.matzzang.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE :cursor IS NULL OR u.id > :cursor ORDER BY u.id ASC")
    List<User> findAllByCursor(@Param("cursor") Long cursor, Pageable pageable);
}