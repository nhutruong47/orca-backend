from typing import Any, Literal

from pydantic import BaseModel, Field


Intent = Literal["PRODUCTION_PLAN", "OPERATION_TASK", "UNKNOWN"]
PriorityText = Literal["LOW", "MEDIUM", "HIGH"]


class ExtractRequest(BaseModel):
    teamId: str | None = None
    text: str = Field(min_length=1)


class ExtractResponse(BaseModel):
    intent: Intent
    confidence: float = Field(ge=0, le=1)
    fields: dict[str, Any] = Field(default_factory=dict)
    missingFields: list[str] = Field(default_factory=list)
    clarifyingQuestion: str | None = None


class TeamMemberContext(BaseModel):
    userId: str
    username: str
    fullName: str | None = None
    jobLabels: list[str] = Field(default_factory=list)


class PlanRequest(BaseModel):
    teamId: str | None = None
    intent: Intent
    fields: dict[str, Any] = Field(default_factory=dict)
    members: list[TeamMemberContext] = Field(default_factory=list)


class TaskDraft(BaseModel):
    title: str
    description: str
    priority: int = Field(ge=1, le=5)
    workload: float = Field(gt=0)
    suggestedAssigneeId: str | None = None
    suggestedAssigneeName: str | None = None
    suggestedReason: str | None = None


class PlanDraftResponse(BaseModel):
    goalTitle: str
    outputTarget: str
    deadline: str | None = None
    priority: int = Field(ge=1, le=5)
    tasks: list[TaskDraft]


class ReviseRequest(BaseModel):
    teamId: str | None = None
    instruction: str = Field(min_length=1)
    draft: PlanDraftResponse
    members: list[TeamMemberContext] = Field(default_factory=list)
