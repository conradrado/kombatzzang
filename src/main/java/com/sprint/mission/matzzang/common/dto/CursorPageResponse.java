package com.sprint.mission.matzzang.common.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {

    public static <T> CursorPageResponse<T> of(List<T> content, Long nextCursor, boolean hasNext) {
        return new CursorPageResponse<>(content, nextCursor, hasNext);
    }
}
