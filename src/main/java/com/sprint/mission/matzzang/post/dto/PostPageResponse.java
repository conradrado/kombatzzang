package com.sprint.mission.matzzang.post.dto;

import com.sprint.mission.matzzang.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostPageResponse(
        @Schema(description = "게시글 목록") List<PostResponse> content,
        @Schema(description = "현재 페이지 번호(0부터)") int page,
        @Schema(description = "페이지 크기") int size,
        @Schema(description = "전체 게시글 수") long totalElements,
        @Schema(description = "전체 페이지 수") int totalPages,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {

    public static PostPageResponse from(Page<Post> page) {
        return new PostPageResponse(
                page.getContent().stream().map(PostResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
