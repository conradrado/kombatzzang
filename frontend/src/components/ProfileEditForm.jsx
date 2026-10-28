import { useState } from 'react';
import { updateUser, updateUserRecord, updateUserRegion } from '../api/client.js';
import { REGIONS, SPORTS, USER_LEVELS, WEIGHT_CLASSES } from '../constants.js';

function Select({ value, onChange, options }) {
  return (
    <select value={value} onChange={(e) => onChange(e.target.value)}>
      {options.map((o) => (
        <option key={o} value={o}>{o}</option>
      ))}
    </select>
  );
}

// 제출 중 disabled, 성공/실패 메시지 처리를 공통으로 담당한다.
function useSubmit(action, onUpdated) {
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    setPending(true);
    setMessage(null);
    setError(null);
    try {
      onUpdated(await action());
      setMessage('저장되었습니다.');
    } catch (err) {
      setError(err.message);
    } finally {
      setPending(false);
    }
  };
  return { pending, message, error, submit };
}

function Feedback({ message, error }) {
  return (
    <>
      {message && <p>{message}</p>}
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
    </>
  );
}

export default function ProfileEditForm({ user, isAdmin, onUpdated }) {
  const [username, setUsername] = useState(user.username ?? '');
  const [sport, setSport] = useState(user.sport ?? 'UNKNOWN');
  const [weightClass, setWeightClass] = useState(user.weightClass ?? 'UNKNOWN');
  const [activityRegion, setActivityRegion] = useState(user.activityRegion ?? 'UNKNOWN');
  const [bio, setBio] = useState(user.bio ?? '');
  const [region, setRegion] = useState(user.region ?? 'UNKNOWN');
  const [wins, setWins] = useState(user.wins ?? 0);
  const [losses, setLosses] = useState(user.losses ?? 0);
  const [level, setLevel] = useState(user.level ?? USER_LEVELS[0]);

  const info = useSubmit(
    () => updateUser(user.id, {
      username, profileImageKey: user.profileImageKey, sport, weightClass, activityRegion, bio,
    }),
    onUpdated,
  );
  const regionForm = useSubmit(() => updateUserRegion(user.id, region), onUpdated);
  const record = useSubmit(
    () => updateUserRecord(user.id, { wins: Number(wins), losses: Number(losses), level }),
    onUpdated,
  );

  return (
    <div>
      <form onSubmit={info.submit}>
        <h3>기본 정보 수정</h3>
        <p><input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="username" required /></p>
        <p>종목 <Select value={sport} onChange={setSport} options={SPORTS} /></p>
        <p>체급 <Select value={weightClass} onChange={setWeightClass} options={WEIGHT_CLASSES} /></p>
        <p>활동 지역 <Select value={activityRegion} onChange={setActivityRegion} options={REGIONS} /></p>
        <p><textarea value={bio} onChange={(e) => setBio(e.target.value)} maxLength={500} placeholder="소개" /></p>
        <button type="submit" disabled={info.pending}>저장</button>
        <Feedback message={info.message} error={info.error} />
      </form>

      <form onSubmit={regionForm.submit}>
        <h3>지역 수정</h3>
        <p><Select value={region} onChange={setRegion} options={REGIONS} /></p>
        <button type="submit" disabled={regionForm.pending}>저장</button>
        <Feedback message={regionForm.message} error={regionForm.error} />
      </form>

      {isAdmin && (
        <form onSubmit={record.submit}>
          <h3>전적 수정</h3>
          <p>
            <input type="number" min="0" value={wins} onChange={(e) => setWins(e.target.value)} required /> 승{' '}
            <input type="number" min="0" value={losses} onChange={(e) => setLosses(e.target.value)} required /> 패
          </p>
          <p><Select value={level} onChange={setLevel} options={USER_LEVELS} /></p>
          <button type="submit" disabled={record.pending}>저장</button>
          <Feedback message={record.message} error={record.error} />
        </form>
      )}
    </div>
  );
}
