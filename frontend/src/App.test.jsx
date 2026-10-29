import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import App from './App.jsx';

describe('App 회원가입 화면 전환', () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
  });

  it('토큰이 없을 때 회원가입 버튼으로 폼에 들어간다', () => {
    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    expect(screen.getByPlaceholderText('닉네임')).toBeInTheDocument();
  });

  it('가입 성공 시 로그인 화면에 완료 메시지를 보여 준다', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 201, json: async () => ({ id: 1 }) });
    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    fireEvent.change(screen.getByPlaceholderText('닉네임'), { target: { value: 'tester' } });
    fireEvent.change(screen.getByPlaceholderText('이메일'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호'), { target: { value: 'password1' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 확인'), { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    expect(await screen.findByText(/회원가입이 완료되었습니다/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
  });

  it('회원가입 화면에서 취소하면 로그인 화면으로 돌아간다', () => {
    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    fireEvent.click(screen.getByRole('button', { name: '로그인으로 돌아가기' }));
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('닉네임')).not.toBeInTheDocument();
  });

  it('가입 실패 시 회원가입 화면에 남아 오류를 보여 준다', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => ({ status: 409, error: 'Conflict', message: '이미 사용 중인 이메일입니다.' }),
    });
    render(<App />);
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    fireEvent.change(screen.getByPlaceholderText('닉네임'), { target: { value: 'tester' } });
    fireEvent.change(screen.getByPlaceholderText('이메일'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호'), { target: { value: 'password1' } });
    fireEvent.change(screen.getByPlaceholderText('비밀번호 확인'), { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: '회원가입' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('이미 사용 중인 이메일입니다.');
    expect(screen.getByPlaceholderText('닉네임')).toBeInTheDocument();
  });
});
