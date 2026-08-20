package com.sprint.mission.matzzang.user.repository;

import com.sprint.mission.matzzang.user.entity.User;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // 입력받은 email로 가입된 유저가 있는지?
    boolean existsByEmail(String email);

    // 입력받은 유저 이름으로 가입된 유저가 있는지?
    boolean existsByUsername(String username);

    // 커서와 Pageable 을 입력받고, 해당 커서 이후의 유저 목록들을 뽑아온다.
    @Query("SELECT u FROM User u WHERE :cursor IS NULL OR u.id > :cursor ORDER BY u.id ASC")
    List<User> findAllByCursor(@Param("cursor") Long cursor, Pageable pageable);
}