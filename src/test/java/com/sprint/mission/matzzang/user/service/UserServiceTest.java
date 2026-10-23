package com.sprint.mission.matzzang.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.common.dto.CursorPageResponse;
import com.sprint.mission.matzzang.user.dto.UserUpdateCommand;
import java.util.List;
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

    @Test
    void 프로필_항목을_수정하면_응답과_엔티티에_반영된다() {
        User user = user();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.updateUser(1L,
                new UserUpdateCommand(null, null, Sport.BJJ, "페더급", "대전", 5, 1, UserLevel.ADVANCED, "소개"));

        assertThat(response.sport()).isEqualTo(Sport.BJJ);
        assertThat(response.activityRegion()).isEqualTo("대전");
        assertThat(response.wins()).isEqualTo(5);
        assertThat(response.level()).isEqualTo(UserLevel.ADVANCED);
        assertThat(user.getBio()).isEqualTo("소개");
    }

    @Test
    void 활동_지역으로_유저를_조회하면_다음_페이지_정보가_함께_반환된다() {
        User a = user();
        User b = user();
        given(userRepository.findActiveByRegionAndCursor(eq("서울"), eq(null), any())).willReturn(List.of(a, b));

        CursorPageResponse<UserResponse> page = userService.getActiveUsers("서울", null, 1);

        assertThat(page.content()).hasSize(1);
        assertThat(page.hasNext()).isTrue();
    }
}
