"""사용법:
    python harness/run.py "<목표>"
    python harness/run.py --goal-file goal.md
    python harness/run.py --resume <run-id>
"""
import argparse
import sys
from datetime import datetime
from pathlib import Path

# 어느 위치에서 실행해도 harness 폴더 안의 모듈(guard, agents 등)을 import 할 수 있도록 경로에 추가
sys.path.insert(0, str(Path(__file__).resolve().parent))

import guard
from agents import AgentError, ClaudeAgents
from config import REPO_ROOT, RUNS_DIR, load_config
from loop import ABORTED, SUCCESS, Harness


def main():
    """CLI 진입점: 인자를 파싱하고 하네스를 새로 시작하거나 이어서 실행한 뒤, 결과를 종료 코드로 반환한다."""
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # Windows cp949 콘솔에서 이모지 출력 오류 방지
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    ap = argparse.ArgumentParser(description="Planner/Generator/Evaluator 루프 하네스")
    ap.add_argument("goal", nargs="?", help="달성할 목표")
    ap.add_argument("--goal-file", help="목표를 담은 파일")
    ap.add_argument("--resume", metavar="RUN_ID", help="중단된 run 이어서 실행")
    ap.add_argument("--config", help="config.toml 경로")
    ap.add_argument("--max-iterations", type=int)
    args = ap.parse_args()

    # 설정 파일을 읽고, CLI 인자가 있으면 config.toml 의 최대 반복 횟수를 덮어쓴다
    cfg = load_config(args.config)
    if args.max_iterations:
        cfg["limits"]["max_iterations"] = args.max_iterations

    # 에이전트가 코드를 직접 수정하므로 main/dev 같은 보호 브랜치에서는 실행을 막는다
    branch = guard.current_branch(REPO_ROOT)
    if branch in cfg["git"]["protected_branches"]:
        sys.exit(f"보호 브랜치({branch})에서는 실행하지 않습니다. feature 브랜치로 이동하세요.")

    try:
        agents = ClaudeAgents(cfg)
        if args.resume:
            # 이어서 실행: 기존 run 폴더에 저장된 상태를 불러와 RUNNING 으로 되돌린다
            run_dir = RUNS_DIR / args.resume
            harness = Harness(cfg, agents, REPO_ROOT, run_dir)
            state = harness.load()
            state["status"] = "RUNNING"
        else:
            # 새 실행: 목표는 --goal-file 이 있으면 파일에서, 없으면 위치 인자에서 가져온다
            goal = Path(args.goal_file).read_text(encoding="utf-8") if args.goal_file else args.goal
            if not goal:
                ap.error("목표 문자열, --goal-file, --resume 중 하나가 필요합니다.")
            # run 폴더 이름은 실행 시각(예: 20261020-153000)으로 만든다
            run_dir = RUNS_DIR / datetime.now().strftime("%Y%m%d-%H%M%S")
            harness = Harness(cfg, agents, REPO_ROOT, run_dir)
            state = harness.new_state(goal)
        print(f"run: {run_dir} (branch: {branch})")
        state = harness.run(state)
    except AgentError as e:
        sys.exit(f"에이전트 오류: {e}\n(--resume 으로 이어서 실행할 수 있습니다)")

    # 종료 코드: 성공 0, 중단(ABORTED) 2, 그 외(반복 한도 초과 등 실패) 1
    sys.exit(0 if state["status"] == SUCCESS else 2 if state["status"] == ABORTED else 1)


if __name__ == "__main__":
    main()
