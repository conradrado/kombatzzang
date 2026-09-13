package com.sprint.mission.matzzang.post.service;

import com.sprint.mission.matzzang.common.dto.CursorPageResponse;
import com.sprint.mission.matzzang.post.constants.PostStatus;
import com.sprint.mission.matzzang.post.dto.PostCreateCommand;
import com.sprint.mission.matzzang.post.dto.PostResponse;
import com.sprint.mission.matzzang.post.dto.PostUpdateCommand;
import com.sprint.mission.matzzang.post.entity.Post;
import com.sprint.mission.matzzang.post.exception.PostNotFoundException;
import com.sprint.mission.matzzang.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse createPost(PostCreateCommand command) {
        Post post = Post.builder()
                .title(command.title())
                .body(command.body())
                .userId(command.userId())
                .status(PostStatus.RECRUITING)
                .build();

        return PostResponse.from(postRepository.save(post));
    }

    public PostResponse getPost(Long postId) {
        return PostResponse.from(findPostById(postId));
    }

    public CursorPageResponse<PostResponse> getPostList(Long cursor, int size) {
        List<Post> posts = postRepository.findAllByCursor(cursor, PageRequest.of(0, size + 1));

        boolean hasNext = posts.size() > size;
        List<Post> content = hasNext ? posts.subList(0, size) : posts;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<PostResponse> responses = content.stream()
                .map(PostResponse::from)
                .toList();

        return CursorPageResponse.of(responses, nextCursor, hasNext);
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
