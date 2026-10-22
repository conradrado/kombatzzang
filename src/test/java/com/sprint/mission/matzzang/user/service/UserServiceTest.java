package com.sprint.mission.matzzang.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.dto.UserRegionUpdateCommand;
import com.sprint.mission.matzzang.user.dto.UserResponse;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user() {
        return User.builder()
                .username("tester")
                .email("tester@example.com")
                .password("encoded")
                .role(UserRole.MEMBER)
                .build();
    }

    @Test
    void 존재하는_유저의_지역을_변경하면_응답과_엔티티에_반영된다() {
        User user = user();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.updateRegion(1L, new UserRegionUpdateCommand(Region.SEOUL));

        assertThat(response.region()).isEqualTo(Region.SEOUL);
        assertThat(user.getRegion()).isEqualTo(Region.SEOUL);
    }

    @Test
    void 이미_지역이_있는_유저의_지역을_다른_값으로_바꾸면_새_값이_반영된다() {
        User user = user();
        user.updateRegion(Region.BUSAN);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.updateRegion(1L, new UserRegionUpdateCommand(Region.JEJU));

        assertThat(response.region()).isEqualTo(Region.JEJU);
        assertThat(user.getRegion()).isEqualTo(Region.JEJU);
    }

    @Test
    void 없는_유저의_지역을_변경하면_UserNotFoundException이_발생한다() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRegion(99L, new UserRegionUpdateCommand(Region.SEOUL)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
