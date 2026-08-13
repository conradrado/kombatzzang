-- Sample data mirrored from the Records blocks in V1__INITIAL_SCHEAM.sql

INSERT INTO users (id, username, email, password, role) VALUES
    (0, 'Alice',   'alice@matzzang.local',   'placeholder', 'ADMIN'),
    (1, 'Bob',     'bob@matzzang.local',     'placeholder', 'MODERATOR'),
    (2, 'Candice', 'candice@matzzang.local', 'placeholder', 'MODERATOR'),
    (3, 'David',   'david@matzzang.local',   'placeholder', 'MEMBER');

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

INSERT INTO follows (following_user_id, followed_user_id, created_at) VALUES
    (1, 0, '2026-01-01'),
    (3, 2, '2026-02-28');

INSERT INTO posts (id, title, user_id) VALUES
    (0, 'Welcome to the forum!', 0),
    (1, 'Guidelines', 1),
    (2, 'Hello all!', 3);

SELECT setval(pg_get_serial_sequence('posts', 'id'), (SELECT MAX(id) FROM posts));
