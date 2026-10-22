package com.sprint.mission.matzzang.post.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.dto.PostUpdateCommand;
import com.sprint.mission.matzzang.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post", description = "게시글 관리 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "로그인한 사용자를 작성자로 게시글을 등록한다.")
    @ApiResponse(responseCode = "201", description = "생성됨")
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody PostCreateCommand command
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(principal.userId(), command));
    }

    @Operation(summary = "게시글 수정", description = "작성자 본인 또는 ADMIN만 수정할 수 있다.")
    @PreAuthorize("@postAccessGuard.isAuthorOrAdmin(authentication, #postId)")
    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody PostUpdateCommand command
    ) {
        return ResponseEntity.ok(postService.updatePost(postId, command));
    }

    @Operation(summary = "게시글 삭제", description = "작성자 본인 또는 ADMIN만 삭제할 수 있다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @PreAuthorize("@postAccessGuard.isAuthorOrAdmin(authentication, #postId)")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@Parameter(description = "게시글 ID") @PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
