package com.sprint.mission.matzzang.post.controller;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostPageResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "게시글 단건 조회")
    @ApiResponse(responseCode = "404", description = "게시글 없음")
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@Parameter(description = "게시글 ID") @PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @Operation(summary = "게시글 목록 조회",
            description = "최신순 페이지 번호 기반 페이지네이션으로 게시글 목록을 조회한다. userId를 주면 해당 작성자의 게시글만 조회한다.")
    @GetMapping
    public ResponseEntity<PostPageResponse> getPostList(
            @Parameter(description = "작성자 ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(postService.getPostList(userId, page, size));
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

    @Operation(summary = "게시글 삭제", description = "ADMIN만 삭제할 수 있다.")
    @ApiResponse(responseCode = "204", description = "삭제됨")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@Parameter(description = "게시글 ID") @PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
