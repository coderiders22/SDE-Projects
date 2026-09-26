from langgraph.graph import StateGraph, END
from app.graph.state import AppState
from app.agents.planner import planner_agent
from app.agents.developer import developer_agent
from app.agents.reviewer import reviewer_agent

# Maximum developer→reviewer cycles before forcing termination.
MAX_ROUNDS = 5


def after_planner(state: AppState) -> str:
    if state.needs_clarification:
        return "end"
    return "developer"


def after_reviewer(state: AppState) -> str:
    if state.needs_clarification:
        return "end"
    if state.approved:
        return "end"
    if state.round >= MAX_ROUNDS:
        return "end"
    return "developer"


def build_graph():
    """
    Compiles the agent graph.

    Normal flow:
        planner → developer → reviewer → END (approved or max rounds reached)
                                  ↑__________↓  (rejected, rounds remaining)

    Clarification flow:
        planner  → END  (task is too ambiguous to plan)
        reviewer → END  (missing info discovered during review)
    """
    workflow = StateGraph(AppState)

    workflow.add_node("planner", planner_agent)
    workflow.add_node("developer", developer_agent)
    workflow.add_node("reviewer", reviewer_agent)

    workflow.set_entry_point("planner")

    workflow.add_conditional_edges(
        "planner",
        after_planner,
        {"developer": "developer", "end": END},
    )

    workflow.add_edge("developer", "reviewer")

    workflow.add_conditional_edges(
        "reviewer",
        after_reviewer,
        {"developer": "developer", "end": END},
    )

    return workflow.compile()
