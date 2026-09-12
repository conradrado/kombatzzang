package com.sprint.mission.matzzang.user.service;

import com.sprint.mission.matzzang.common.dto.CursorPageResponse;
import com.sprint.mission.matzzang.user.constants.UserRole;
import com.sprint.mission.matzzang.user.dto.UserCreateCommand;
import com.sprint.mission.matzzang.user.dto.UserResponse;
import com.sprint.mission.matzzang.user.dto.UserUpdateCommand;
import com.sprint.mission.matzzang.user.entity.User;
import com.sprint.mission.matzzang.user.exception.DuplicateEmailException;
import com.sprint.mission.matzzang.user.exception.DuplicateUsernameException;
import com.sprint.mission.matzzang.user.exception.UserNotFoundException;
import com.sprint.mission.matzzang.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new DuplicateEmailException(command.email());
        }
        if (userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }

        User user = User.builder()
                .username(command.username())
                .email(command.email())
                .password(passwordEncoder.encode(command.password())) // 평문으로 들어온 비밀번호를 암호화하여 유저 객체 생성
                .profileImageKey(command.profileImageKey())
                .role(UserRole.MEMBER) // 초기 유저 생성은 Member 권한으로 초기화
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUser(Long userId) {
        return UserResponse.from(findUserById(userId));
    }

    public CursorPageResponse<UserResponse> getUserList(Long cursor, int size) {
        List<User> users = userRepository.findAllByCursor(cursor, PageRequest.of(0, size + 1));

        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<UserResponse> responses = content.stream()
                .map(UserResponse::from)
                .toList();

        return CursorPageResponse.of(responses, nextCursor, hasNext);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateCommand command) {
        User user = findUserById(userId);

        if (command.username() != null
                && !command.username().equals(user.getUsername())
                && userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }

        user.updateProfile(command.username(), command.profileImageKey());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.delete(findUserById(userId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
