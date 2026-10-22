package com.sprint.mission.matzzang.post.service;

import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.dto.PostUpdateCommand;
import com.sprint.mission.matzzang.post.entity.Post;
import com.sprint.mission.matzzang.post.exception.PostNotFoundException;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse createPost(Long userId, PostCreateCommand command) {
        Post post = Post.builder()
                .title(command.title())
                .body(command.body())
                .userId(userId)
                .status(PostStatus.RECRUITING) // 초기 게시글은 모집 중 상태로 생성
                .build();

        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostUpdateCommand command) {
        Post post = findPostById(postId);
        post.updateContent(command.title(), command.body());
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        postRepository.delete(findPostById(postId));
    }

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }
}
