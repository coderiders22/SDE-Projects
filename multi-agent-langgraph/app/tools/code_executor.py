import subprocess
import tempfile

def run_python_code(code: str):
    """
    Execute Python code and return the output.
    """
    with tempfile.NamedTemporaryFile(delete=False, suffix=".py") as f:
        f.write(code.encode())
        file_path = f.name

    result = subprocess.run(["python", file_path], capture_output=True, text=True)

    return result.stdout or result.stderr