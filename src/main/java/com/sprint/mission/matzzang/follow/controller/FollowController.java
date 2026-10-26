package com.sprint.mission.matzzang.follow.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.follow.dto.FollowCreateRequest;
import com.sprint.mission.matzzang.follow.dto.FollowResponse;
import com.sprint.mission.matzzang.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Follow", description = "팔로우 관리 API")
@RestController
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "팔로우", description = "로그인한 사용자가 대상 사용자를 팔로우한다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @PostMapping("/follow")
    public ResponseEntity<FollowResponse> follow(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "팔로우할 사용자 ID") @PathVariable Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(followService.follow(principal.userId(), userId));
    }

    @Operation(summary = "언팔로우", description = "로그인한 사용자가 대상 사용자의 팔로우를 취소한다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Parameter(description = "언팔로우할 사용자 ID") @PathVariable Long userId
            @Valid @RequestBody FollowCreateRequest request
    ) {
        followService.unfollow(principal.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "팔로워 목록 조회", description = "대상 사용자를 팔로우하는 사용자 목록을 조회한다.")
    @GetMapping("/followers")
    public ResponseEntity<List<FollowResponse>> getFollowers(
            @Parameter(description = "사용자 ID") @PathVariable Long userId
    ) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    @Operation(summary = "팔로잉 목록 조회", description = "대상 사용자가 팔로우하는 사용자 목록을 조회한다.")
    @GetMapping("/followings")
    public ResponseEntity<List<FollowResponse>> getFollowings(
            @Parameter(description = "사용자 ID") @PathVariable Long userId
    ) {
        return ResponseEntity.ok(followService.getFollowings(userId));
    }
}
