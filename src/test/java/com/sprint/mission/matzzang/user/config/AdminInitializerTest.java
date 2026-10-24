package com.sprint.mission.matzzang.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInitializer adminInitializer;

    @BeforeEach
    void setUp() {
        adminInitializer = new AdminInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(adminInitializer, "email", "admin@example.com");
        ReflectionTestUtils.setField(adminInitializer, "username", "admin");
        ReflectionTestUtils.setField(adminInitializer, "password", "raw-password");
    }

    @Test
    void 설정값이_있으면_암호화된_비밀번호로_ADMIN_계정을_생성한다() {
        given(userRepository.existsByEmail("admin@example.com")).willReturn(false);
        given(userRepository.existsByUsername("admin")).willReturn(false);
        given(passwordEncoder.encode("raw-password")).willReturn("encoded-password");

        adminInitializer.run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@example.com");
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void 이메일이_비어_있으면_생성하지_않는다() {
        ReflectionTestUtils.setField(adminInitializer, "email", "");

        adminInitializer.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void 비밀번호가_비어_있으면_생성하지_않는다() {
        ReflectionTestUtils.setField(adminInitializer, "password", " ");

        adminInitializer.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void 이미_존재하는_이메일이면_생성하지_않는다() {
        given(userRepository.existsByEmail("admin@example.com")).willReturn(true);

        adminInitializer.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void 이미_존재하는_username이면_생성하지_않는다() {
        given(userRepository.existsByEmail("admin@example.com")).willReturn(false);
        given(userRepository.existsByUsername("admin")).willReturn(true);

        adminInitializer.run(null);

        verify(userRepository, never()).save(any());
    }
}
