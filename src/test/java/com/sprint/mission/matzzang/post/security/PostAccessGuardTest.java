package com.sprint.mission.matzzang.post.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.matzzang.auth.jwt.JwtPrincipal;
import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.entity.Post;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import com.sprint.mission.matzzang.user.constants.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class PostAccessGuardTest {

    @Mock
    private Authentication authentication;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostAccessGuard postAccessGuard;

    private Post postOf(Long userId) {
        return Post.builder().title("제목").body("내용").userId(userId).status(PostStatus.RECRUITING).build();
    }

    @Test
    void 작성자_본인이면_true를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(1L, UserRole.MEMBER));
        given(postRepository.findById(10L)).willReturn(Optional.of(postOf(1L)));

        assertThat(postAccessGuard.isAuthorOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void 다른_사용자면_false를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(2L, UserRole.MEMBER));
        given(postRepository.findById(10L)).willReturn(Optional.of(postOf(1L)));

        assertThat(postAccessGuard.isAuthorOrAdmin(authentication, 10L)).isFalse();
    }

    @Test
    void ADMIN이면_true를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(2L, UserRole.ADMIN));

        assertThat(postAccessGuard.isAuthorOrAdmin(authentication, 10L)).isTrue();
    }

    @Test
    void Authentication이_null이면_false를_반환한다() {
        assertThat(postAccessGuard.isAuthorOrAdmin(null, 10L)).isFalse();
    }

    @Test
    void principal이_JwtPrincipal이_아니면_false를_반환한다() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        assertThat(postAccessGuard.isAuthorOrAdmin(authentication, 10L)).isFalse();
    }

    @Test
    void 게시글이_없으면_true를_반환한다() {
        given(authentication.getPrincipal()).willReturn(new JwtPrincipal(1L, UserRole.MEMBER));
        given(postRepository.findById(10L)).willReturn(Optional.empty());

        assertThat(postAccessGuard.isAuthorOrAdmin(authentication, 10L)).isTrue();
    }
}
