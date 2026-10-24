package com.sprint.mission.matzzang.user.config;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 설정값(admin.email / admin.username / admin.password)으로 ADMIN 계정을 생성한다.
 * 이메일 또는 비밀번호가 비어 있으면 생성하지 않으며, 이미 존재하는 계정은 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String email;

    @Value("${admin.username:admin}")
    private String username;

    @Value("${admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("admin.email/admin.password 미설정: ADMIN 계정 초기화를 건너뜀");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            log.info("ADMIN 초기화 건너뜀: 이미 존재하는 이메일 ({})", email);
            return;
        }
        if (userRepository.existsByUsername(username)) {
            log.warn("ADMIN 초기화 건너뜀: 이미 존재하는 username ({})", username);
            return;
        }

        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .region(Region.UNKNOWN)
                .sport(Sport.UNKNOWN)
                .weightClass(WeightClass.UNKNOWN)
                .activityRegion(Region.UNKNOWN)
                .build());
        log.info("ADMIN 계정 생성 완료 ({})", email);
    }
}
