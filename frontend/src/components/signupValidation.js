const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// 백엔드 UserCreateCommand 제약과 맞춘다. 위반한 필드만 키로 가진 객체를 반환한다.
export function validateSignup({ username, email, password, passwordConfirm }) {
  const errors = {};

  const name = (username ?? '').trim();
  if (!name) {
    errors.username = '닉네임을 입력해 주세요.';
  } else if (name.length < 2 || name.length > 20) {
    errors.username = '닉네임은 2자 이상 20자 이하여야 합니다.';
  }

  const mail = (email ?? '').trim();
  if (!mail) {
    errors.email = '이메일을 입력해 주세요.';
  } else if (!EMAIL_PATTERN.test(mail)) {
    errors.email = '이메일 형식이 올바르지 않습니다.';
  }

  if (!password || password.length < 8 || password.length > 64) {
    errors.password = '비밀번호는 8자 이상 64자 이하여야 합니다.';
  }

  if (passwordConfirm !== password) {
    errors.passwordConfirm = '비밀번호가 일치하지 않습니다.';
  }

  return errors;
}
