import tomllib
from pathlib import Path

HARNESS_DIR = Path(__file__).resolve().parent
REPO_ROOT = HARNESS_DIR.parent
RUNS_DIR = HARNESS_DIR / "runs"
PROMPTS_DIR = HARNESS_DIR / "prompts"

# config.toml 파일에 있는 속성들을 로드하는 함수. run.py에서 실행됩니다.
def load_config(path=None):
    # 매개변수로 경로가 따로 지정되면 해당 파일에서 속성을 읽고, 없으면 config.toml 파일을 찾아 읽습니다.
    path = Path(path) if path else HARNESS_DIR / "config.toml"
    with open(path, "rb") as f:
        return tomllib.load(f) # config.toml 파일을 딕셔너리 형태로 바꾸어 리턴합니다.
