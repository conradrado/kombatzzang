package com.sprint.mission.matzzang.follow.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.follow.dto.FollowCreateRequest;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Follow", description = "팔로우 관리 API")
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우", description = "로그인한 사용자가 대상 사용자를 팔로우한다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @PostMapping
    public ResponseEntity<FollowResponse> createFollow(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody FollowCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.createFollow(principal.userId(), request));
    }
}
