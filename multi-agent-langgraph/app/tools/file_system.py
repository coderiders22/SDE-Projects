import os


def write_file(filename: str, content: str) -> str:
    """
    Write content to a file in the workspace directory.
    """

    os.makedirs("workspace", exist_ok=True)

    path = os.path.join("workspace", filename)

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

    return f"File written to {path}"