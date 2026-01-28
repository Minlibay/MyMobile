from pydantic import BaseModel, Field
from typing import List


class RegisterRequest(BaseModel):
    login: str = Field(min_length=3, max_length=64)
    password: str = Field(min_length=6, max_length=128)


class LoginRequest(BaseModel):
    login: str
    password: str
    device_id: str | None = None


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str


class RefreshRequest(BaseModel):
    refresh_token: str
    device_id: str | None = None


class LogoutRequest(BaseModel):
    refresh_token: str


class UserMeResponse(BaseModel):
    id: int
    login: str


class AdUnitUpsert(BaseModel):
    network: str
    placement: str
    ad_unit_id: str
    enabled: bool = True
    android_min_version: int | None = None
    android_max_version: int | None = None


class AdsConfigResponse(BaseModel):
    network: str
    units: dict[str, str]


class ProfileRequest(BaseModel):
    height_cm: int = Field(gt=0, le=300)
    weight_kg: float = Field(gt=0, le=500)
    age: int = Field(gt=0, le=150)
    sex: str = Field(pattern="^(male|female)$")


class ProfileResponse(ProfileRequest):
    id: int
    user_id: int
    created_at: str
    updated_at: str


class UserSettingsRequest(BaseModel):
    calorie_mode: str = Field(pattern="^(maintain|lose|gain)$")
    step_goal: int = Field(gt=0)
    calorie_goal_override: int | None = Field(None, gt=0)
    reminders_enabled: bool = True


class UserSettingsResponse(UserSettingsRequest):
    id: int
    user_id: int
    updated_at: str


class FamilyRequest(BaseModel):
    name: str = Field(min_length=3, max_length=64)


class FamilyMemberResponse(BaseModel):
    user_id: int
    login: str
    joined_at: str


class FamilyResponse(FamilyRequest):
    id: int
    admin_user_id: int
    created_at: str
    members: List[FamilyMemberResponse] = Field(default_factory=list)


class InviteUserRequest(BaseModel):
    login: str = Field(min_length=3, max_length=64)


class JoinFamilyRequest(BaseModel):
    family_name: str = Field(min_length=3, max_length=64)


class StepUpsertRequest(BaseModel):
    date_epoch_day: int
    steps: int = Field(ge=0)


class StepEntryResponse(BaseModel):
    date_epoch_day: int
    steps: int
    updated_at: str


class WaterCreateRequest(BaseModel):
    date_epoch_day: int
    amount_ml: int = Field(gt=0)


class WaterEntryResponse(BaseModel):
    id: int
    date_epoch_day: int
    amount_ml: int
    created_at: str


class WeightUpsertRequest(BaseModel):
    date_epoch_day: int
    weight_kg: float = Field(gt=0, le=500)


class WeightEntryResponse(BaseModel):
    date_epoch_day: int
    weight_kg: float
    updated_at: str


class SmokeStatusRequest(BaseModel):
    started_at: str
    is_active: bool = True
    pack_price: float = Field(ge=0)
    packs_per_day: float = Field(ge=0)


class SmokeStatusResponse(SmokeStatusRequest):
    updated_at: str


class FoodCreateRequest(BaseModel):
    date_epoch_day: int
    title: str = Field(min_length=1, max_length=256)
    calories: int = Field(gt=0)


class FoodEntryResponse(BaseModel):
    id: int
    date_epoch_day: int
    title: str
    calories: int
    created_at: str


class TrainingCreateRequest(BaseModel):
    date_epoch_day: int
    title: str = Field(min_length=1, max_length=256)
    description: str | None = Field(default=None, max_length=512)
    calories_burned: int = Field(ge=0)
    duration_minutes: int = Field(ge=0)


class TrainingEntryResponse(BaseModel):
    id: int
    date_epoch_day: int
    title: str
    description: str | None = None
    calories_burned: int
    duration_minutes: int
    created_at: str







