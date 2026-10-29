import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SignupForm from './SignupForm.jsx';

const fill = (username, email, password, confirm) => {
  fireEvent.change(screen.getByPlaceholderText('닉네임'), { target: { value: username } });
  fireEvent.change(screen.getByPlaceholderText('이메일'), { target: { value: email } });
  fireEvent.change(screen.getByPlaceholderText('비밀번호'), { target: { value: password } });
  fireEvent.change(screen.getByPlaceholderText('비밀번호 확인'), { target: { value: confirm } });
  fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
};

const mockResponse = (status, body) => ({
  ok: status >= 200 && status < 300,
  status,
  json: async () => body,
});

describe('SignupForm', () => {
  let onSignup;
  let onCancel;

  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
    onSignup = vi.fn();
    onCancel = vi.fn();
    render(<SignupForm onSignup={onSignup} onCancel={onCancel} />);
  });

  it('잘못된 입력이면 에러를 표시하고 fetch를 호출하지 않는다', async () => {
    fill('a', 'bad', 'short', 'other');
    expect(await screen.findByText('닉네임은 2자 이상 20자 이하여야 합니다.')).toBeInTheDocument();
    expect(screen.getByText('비밀번호가 일치하지 않습니다.')).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
    expect(onSignup).not.toHaveBeenCalled();
  });

  it('유효한 입력이면 POST /api/users 를 호출하고 onSignup 을 부른다', async () => {
    global.fetch.mockResolvedValue(mockResponse(201, { id: 1 }));
    fill('tester', 'a@b.com', 'password1', 'password1');
    await waitFor(() => expect(onSignup).toHaveBeenCalledTimes(1));
    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('/api/users');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({
      username: 'tester',
      email: 'a@b.com',
      password: 'password1',
    });
  });

  it('409 응답의 메시지를 alert 로 표시한다', async () => {
    global.fetch.mockResolvedValue(
      mockResponse(409, { status: 409, error: 'Conflict', message: '이미 사용 중인 이메일입니다.' }));
    fill('tester', 'a@b.com', 'password1', 'password1');
    expect(await screen.findByRole('alert')).toHaveTextContent('이미 사용 중인 이메일입니다.');
    expect(onSignup).not.toHaveBeenCalled();
  });

  it('400 등 다른 오류의 메시지도 표시한다', async () => {
    global.fetch.mockResolvedValue(
      mockResponse(400, { status: 400, error: 'Bad Request', message: '잘못된 요청입니다.' }));
    fill('tester', 'a@b.com', 'password1', 'password1');
    expect(await screen.findByRole('alert')).toHaveTextContent('잘못된 요청입니다.');
    expect(onSignup).not.toHaveBeenCalled();
  });

  it('본문이 JSON 이 아닌 409 는 기본 중복 안내 문구를 표시한다', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => {
        throw new Error('not json');
      },
    });
    fill('tester', 'a@b.com', 'password1', 'password1');
    expect(await screen.findByRole('alert'))
      .toHaveTextContent('이미 사용 중인 이메일 또는 닉네임입니다.');
    expect(onSignup).not.toHaveBeenCalled();
  });

  it('닉네임과 이메일의 앞뒤 공백을 제거해 전송한다', async () => {
    global.fetch.mockResolvedValue(mockResponse(201, { id: 1 }));
    fill('  tester  ', '  a@b.com ', 'password1', 'password1');
    await waitFor(() => expect(onSignup).toHaveBeenCalledTimes(1));
    const [, options] = global.fetch.mock.calls[0];
    expect(JSON.parse(options.body)).toMatchObject({ username: 'tester', email: 'a@b.com' });
  });

  it('요청 진행 중에는 회원가입 버튼이 비활성화된다', async () => {
    global.fetch.mockReturnValue(new Promise(() => {}));
    fill('tester', 'a@b.com', 'password1', 'password1');
    await waitFor(() =>
      expect(screen.getByRole('button', { name: '회원가입' })).toBeDisabled());
  });

  it('로그인으로 돌아가기를 누르면 onCancel 을 부른다', () => {
    fireEvent.click(screen.getByRole('button', { name: '로그인으로 돌아가기' }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
