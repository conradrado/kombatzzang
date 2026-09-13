package com.sprint.mission.matzzang.user.controller;

import com.sprint.mission.matzzang.common.dto.CursorPageResponse;
import com.sprint.mission.matzzang.user.dto.UserCreateCommand;
import com.sprint.mission.matzzang.user.dto.UserResponse;
import com.sprint.mission.matzzang.user.dto.UserUpdateCommand;
import com.sprint.mission.matzzang.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 생성", description = "새로운 사용자를 등록한다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(command));
    }

    @Operation(summary = "사용자 단건 조회")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@Parameter(description = "사용자 ID") @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @Operation(summary = "사용자 목록 조회", description = "커서 기반 페이지네이션으로 사용자 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<CursorPageResponse<UserResponse>> getUserList(
            @Parameter(description = "이전 페이지 마지막 사용자 ID") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.getUserList(cursor, size));
    }

    @Operation(summary = "사용자 정보 수정")
    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @RequestBody UserUpdateCommand command
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, command));
    }

    @Operation(summary = "사용자 삭제")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@Parameter(description = "사용자 ID") @PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
