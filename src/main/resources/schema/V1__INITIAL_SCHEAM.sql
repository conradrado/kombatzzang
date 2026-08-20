ㅇ// Use DBML to define your database structure
// Docs: https://dbml.dbdiagram.io/docs

Enum fight_rule {
  MMA
  BOXING
  MUAY_THAI
}

Table follows {
  following_user_id integer [pk]
  followed_user_id integer [pk]
  created_at timestamp
}

Table users {
  id integer [primary key]
  username varchar
  email varchar
  password varchar
  profile_image varchar
  role varchar
  created_at timestamp
}

Table userStat {
  user_id integer [primary key]
  win integer
  lose integer
  rank integer
  weight_class varchar
}

Table posts {
  id integer [primary key]
  title varchar
  body text [note: 'Content of the post']
  user_id integer [not null]
  status varchar
  created_at timestamp
}

Table fights {
  id integer [primary key]
  fighter_1_id integer
  fighter_2_id integer
  referee_id integer
  winner_id integer
  weight_class varchar
  status varchar
  rounds integer
  rule enum
  fight_at timestamp
  created_at timestamp
}


Ref user_posts: posts.user_id > users.id // many-to-one

Ref: users.id < follows.following_user_id

Ref: users.id < follows.followed_user_id

Records users(id, username, role) {
  0, 'Alice', 'admin'
  1, 'Bob', 'moderator'
  2, 'Candice', 'moderator'
  3, 'David', 'member'
}

Records follows(following_user_id, followed_user_id, created_at) {
  1, 0, '2026-01-01'
  3, 2, '2026-02-28'
}

Records posts(id, title, user_id) {
  0, 'Welcome to the forum!', 0
  1, 'Guidelines', 1
  2, 'Hello all!', 3
}



Ref: "users"."id" < "fights"."fighter_2_id"

Ref: "users"."id" < "fights"."referee_id"

Ref: "users"."id" < "fights"."fighter_1_id"

Ref: "users"."id" - "userStat"."user_id"

Ref: "users"."id" < "fights"."winner_id"sw2
