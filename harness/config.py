import tomllib
from pathlib import Path

HARNESS_DIR = Path(__file__).resolve().parent
REPO_ROOT = HARNESS_DIR.parent
RUNS_DIR = HARNESS_DIR / "runs"
PROMPTS_DIR = HARNESS_DIR / "prompts"


def load_config(path=None):
    path = Path(path) if path else HARNESS_DIR / "config.toml"
    with open(path, "rb") as f:
        return tomllib.load(f)
