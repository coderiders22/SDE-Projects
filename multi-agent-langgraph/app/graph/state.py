from pydantic import BaseModel
from typing import List, Optional

# Structured output schemas — each agent is forced to return one of these via .with_structured_output()

class PlannerOut(BaseModel):
    steps: List[str]
    needs_clarification: bool
    clarification_question: Optional[str]

class DeveloperOut(BaseModel):
    code: str
    tool: Optional[str]        # "run_python_code", "write_file", or "run_shell_command"
    filename: Optional[str]    # only set when tool="write_file"

class ReviewerOut(BaseModel):
    approved: bool
    feedback: str
    needs_clarification: bool
    clarification_question: Optional[str]

class Message(BaseModel):
    agent: str   # "planner", "developer", or "reviewer"
    content: str

# Shared state passed between all agents by LangGraph.
class AppState(BaseModel):
    task: str
    discussion: List[Message] = []

    plan: Optional[List[str]] = None
    code: Optional[str] = None
    execution_result: Optional[str] = None  # stdout/stderr or write_file confirmation
    tool_used: Optional[str] = None
    filename: Optional[str] = None          # set when tool_used="write_file"

    feedback: Optional[str] = None
    approved: Optional[bool] = None

    needs_clarification: bool = False
    clarification_question: Optional[str] = None

    round: int = 0  # incremented once per complete developer→reviewer cycle
