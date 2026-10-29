import { render, screen } from '@testing-library/react';

describe('테스트 환경', () => {
  it('jsdom 과 Testing Library, jest-dom 매처가 동작한다', () => {
    render(<button>확인</button>);

    expect(screen.getByRole('button', { name: '확인' })).toBeInTheDocument();
  });
});
