package com.sprint.mission.matzzang.post.security;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import com.sprint.mission.matzzang.user.constants.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostAccessGuard {

    private final PostRepository postRepository;

    public boolean isAuthorOrAdmin(Authentication authentication, Long postId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return false;
        }
        if (principal.role() == UserRole.ADMIN) {
            return true;
        }
        // 게시글이 없으면 통과시켜 서비스에서 PostNotFoundException(404)을 던지게 한다
        return postRepository.findById(postId)
                .map(post -> post.getUserId().equals(principal.userId()))
                .orElse(true);
    }
}
