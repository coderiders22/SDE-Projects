import subprocess


def run_shell_command(command: str) -> str:
    """Executes a shell command and returns stdout/stderr."""
    result = subprocess.run(
        command,
        shell=True,
        capture_output=True,
        text=True,
        timeout=30,
    )
    return result.stdout or result.stderr
