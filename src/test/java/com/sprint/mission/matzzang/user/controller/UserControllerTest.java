package com.sprint.mission.matzzang.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.matzzang.common.dto.CursorPageResponse;
import com.sprint.mission.matzzang.common.exception.GlobalExceptionHandler;
import com.sprint.mission.matzzang.user.constants.Sport;
import com.sprint.mission.matzzang.user.constants.WeightClass;
import com.sprint.mission.matzzang.user.constants.Region;
import com.sprint.mission.matzzang.user.constants.UserLevel;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.dto.UserResponse;
import com.sprint.mission.matzzang.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse response() {
        return new UserResponse(1L, "fighter", "f@test.com", null, UserRole.MEMBER, null,
                Sport.BOXING, WeightClass.LIGHT, Region.SEOUL, 3, 1, UserLevel.AMATEUR, "안녕", null, null);
    }

    @Test
    void 프로필_항목을_담아_수정하면_200과_프로필이_반환된다() throws Exception {
        given(userService.updateUser(eq(1L), any())).willReturn(response());

        mockMvc.perform(patch("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"BOXING\",\"activityRegion\":\"SEOUL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sport").value("BOXING"))
                .andExpect(jsonPath("$.activityRegion").value("SEOUL"));
    }

    @Test
    void 전적과_레벨을_담아_수정하면_200과_값이_반환된다() throws Exception {
        given(userService.updateRecord(eq(1L), any())).willReturn(response());

        mockMvc.perform(patch("/api/users/1/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wins\":3,\"losses\":1,\"level\":\"AMATEUR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wins").value(3))
                .andExpect(jsonPath("$.level").value("AMATEUR"));
    }

    @Test
    void 전적이_음수면_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/users/1/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"wins\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 자기소개가_500자를_넘으면_400이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 활동_지역으로_조회하면_200과_커서_페이지가_반환된다() throws Exception {
        given(userService.getActiveUsers(Region.SEOUL, null, 20))
                .willReturn(CursorPageResponse.of(List.of(response()), 1L, true));

        mockMvc.perform(get("/api/users/active").param("activityRegion", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].activityRegion").value("SEOUL"))
                .andExpect(jsonPath("$.nextCursor").value(1))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void 활동_지역_없이_조회하면_400이_반환된다() throws Exception {
        mockMvc.perform(get("/api/users/active"))
                .andExpect(status().isBadRequest());
    }
}
