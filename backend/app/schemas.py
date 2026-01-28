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
    appodeal_app_key: str | None = None
    appodeal_enabled: bool = False
    appodeal_banner_enabled: bool = True
    appodeal_interstitial_enabled: bool = True
    appodeal_rewarded_enabled: bool = True


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
    target_weight_kg: float | None = Field(None, gt=0, le=500)
    reminders_enabled: bool = True


class UserSettingsResponse(UserSettingsRequest):
    id: int
    user_id: int
    privacy_policy_accepted_at: str | None = None
    privacy_policy_accepted_policy_updated_at: str | None = None
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


class BookCreateRequest(BaseModel):
    title: str = Field(min_length=1, max_length=256)
    author: str | None = Field(default=None, max_length=256)
    total_pages: int = Field(ge=0)


class BookProgressRequest(BaseModel):
    pages_read: int = Field(ge=0)


class BookEntryResponse(BaseModel):
    id: int
    title: str
    author: str | None = None
    total_pages: int
    pages_read: int
    created_at: str


class XpEventCreateRequest(BaseModel):
    date_epoch_day: int
    type: str = Field(min_length=1, max_length=64)
    points: int = Field(ge=0)
    note: str | None = Field(default=None, max_length=256)


class XpEventResponse(BaseModel):
    id: int
    date_epoch_day: int
    type: str
    points: int
    note: str | None = None
    created_at: str


class XpDailyAggregateResponse(BaseModel):
    date_epoch_day: int
    total_points: int


class XpTotalResponse(BaseModel):
    total_points: int
    level: int


class UserAchievementResponse(BaseModel):
    id: int
    code: str
    created_at: str


class SyncBatchItem(BaseModel):
    entity_type: str = Field(min_length=1, max_length=64)
    action: str = Field(min_length=1, max_length=32)
    payload: dict


class SyncBatchRequest(BaseModel):
    items: List[SyncBatchItem]


class SyncBatchResponse(BaseModel):
    processed: int
    failed: int
    errors: List[str] = Field(default_factory=list)


class AdminSettingsRequest(BaseModel):
    openrouter_api_key: str | None = Field(default=None, max_length=256)
    openrouter_model: str | None = Field(default=None, max_length=128)
    appodeal_app_key: str | None = Field(default=None, max_length=256)
    appodeal_enabled: bool | None = None
    appodeal_banner_enabled: bool | None = None
    appodeal_interstitial_enabled: bool | None = None
    appodeal_rewarded_enabled: bool | None = None
    privacy_policy_text: str | None = None


class AdminSettingsResponse(BaseModel):
    openrouter_api_key: str | None = None
    openrouter_model: str | None = None
    appodeal_app_key: str | None = None
    appodeal_enabled: bool = False
    appodeal_banner_enabled: bool = True
    appodeal_interstitial_enabled: bool = True
    appodeal_rewarded_enabled: bool = True
    privacy_policy_text: str | None = None
    privacy_policy_updated_at: str | None = None
    updated_at: str


class PrivacyPolicyResponse(BaseModel):
    text: str
    updated_at: str


class PrivacyPolicyAcceptResponse(BaseModel):
    accepted_at: str
    policy_updated_at: str







