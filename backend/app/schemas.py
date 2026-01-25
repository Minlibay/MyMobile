from pydantic import BaseModel, Field


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












