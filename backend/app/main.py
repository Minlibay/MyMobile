import datetime as dt

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.db import Base, engine, get_db
from app.models import AdUnit, Family, FamilyMember, Profile, RefreshSession, User, UserSettings
from app.schemas import (
    AdUnitUpsert,
    AdsConfigResponse,
    FamilyMemberResponse,
    FamilyRequest,
    FamilyResponse,
    LoginRequest,
    LogoutRequest,
    ProfileRequest,
    ProfileResponse,
    RefreshRequest,
    RegisterRequest,
    TokenPair,
    UserMeResponse,
    UserSettingsRequest,
    UserSettingsResponse,
)
from app.security import (
    create_access_token,
    decode_access_token,
    hash_password,
    hash_refresh_token,
    new_refresh_token,
    verify_password,
)
from app.settings import settings


app = FastAPI(title="Zhivoy API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

bearer = HTTPBearer(auto_error=False)


def now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def require_user(
    creds: HTTPAuthorizationCredentials | None = Depends(bearer),
    db: Session = Depends(get_db),
) -> User:
    if creds is None:
        raise HTTPException(status_code=401, detail="Unauthorized")
    try:
        payload = decode_access_token(creds.credentials)
        user_id = int(payload["sub"])
    except Exception:
        raise HTTPException(status_code=401, detail="Unauthorized")
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=401, detail="Unauthorized")
    return user


def require_admin(x_admin_key: str | None) -> None:
    if not x_admin_key or x_admin_key != settings.ADMIN_API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/auth/register", response_model=UserMeResponse)
def register(req: RegisterRequest, db: Session = Depends(get_db)) -> UserMeResponse:
    try:
        existing = db.execute(select(User).where(User.login == req.login)).scalar_one_or_none()
        if existing is not None:
            raise HTTPException(status_code=409, detail="Login already exists")
        user = User(login=req.login, password_hash=hash_password(req.password))
        db.add(user)
        db.commit()
        db.refresh(user)
        return UserMeResponse(id=user.id, login=user.login)
    except HTTPException:
        raise
    except Exception as e:
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@app.post("/auth/login", response_model=TokenPair)
def login(req: LoginRequest, db: Session = Depends(get_db)) -> TokenPair:
    user = db.execute(select(User).where(User.login == req.login)).scalar_one_or_none()
    if user is None or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    refresh_token = new_refresh_token()
    refresh_hash = hash_refresh_token(refresh_token)
    expires_at = now() + dt.timedelta(seconds=settings.REFRESH_TTL_SECONDS)
    session = RefreshSession(
        user_id=user.id,
        refresh_hash=refresh_hash,
        device_id=req.device_id,
        expires_at=expires_at,
        created_at=now(),
        last_used_at=now(),
    )
    db.add(session)
    db.commit()

    return TokenPair(access_token=create_access_token(user.id), refresh_token=refresh_token)


@app.post("/auth/refresh", response_model=TokenPair)
def refresh(req: RefreshRequest, db: Session = Depends(get_db)) -> TokenPair:
    token_hash = hash_refresh_token(req.refresh_token)
    session = db.execute(select(RefreshSession).where(RefreshSession.refresh_hash == token_hash)).scalar_one_or_none()
    if session is None:
        raise HTTPException(status_code=401, detail="Invalid refresh")
    if session.revoked_at is not None:
        raise HTTPException(status_code=401, detail="Invalid refresh")
    if session.expires_at <= now():
        raise HTTPException(status_code=401, detail="Expired refresh")
    if req.device_id and session.device_id and req.device_id != session.device_id:
        raise HTTPException(status_code=401, detail="Invalid refresh")

    user = db.get(User, session.user_id)
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid refresh")

    # rotate: revoke old session
    session.revoked_at = now()
    session.last_used_at = now()

    new_token = new_refresh_token()
    new_hash = hash_refresh_token(new_token)
    new_session = RefreshSession(
        user_id=user.id,
        refresh_hash=new_hash,
        device_id=session.device_id,
        expires_at=now() + dt.timedelta(seconds=settings.REFRESH_TTL_SECONDS),
        created_at=now(),
        last_used_at=now(),
    )
    db.add(new_session)
    db.commit()

    return TokenPair(access_token=create_access_token(user.id), refresh_token=new_token)


@app.post("/auth/logout")
def logout(req: LogoutRequest, db: Session = Depends(get_db)) -> dict[str, str]:
    token_hash = hash_refresh_token(req.refresh_token)
    session = db.execute(select(RefreshSession).where(RefreshSession.refresh_hash == token_hash)).scalar_one_or_none()
    if session is not None and session.revoked_at is None:
        session.revoked_at = now()
        session.last_used_at = now()
        db.commit()
    return {"status": "ok"}


@app.get("/users/me", response_model=UserMeResponse)
def me(user: User = Depends(require_user)) -> UserMeResponse:
    return UserMeResponse(id=user.id, login=user.login)


@app.get("/ads/config", response_model=AdsConfigResponse)
def ads_config(
    platform: str = "android",
    appVersion: int = 1,
    network: str = "yandex",
    db: Session = Depends(get_db),
) -> AdsConfigResponse:
    # platform reserved for future use
    _ = platform

    q = select(AdUnit).where(AdUnit.network == network, AdUnit.enabled == True)  # noqa: E712
    units = db.execute(q).scalars().all()
    filtered = []
    for u in units:
        if u.android_min_version is not None and appVersion < u.android_min_version:
            continue
        if u.android_max_version is not None and appVersion > u.android_max_version:
            continue
        filtered.append(u)

    return AdsConfigResponse(network=network, units={u.placement: u.ad_unit_id for u in filtered})


@app.get("/profile/me", response_model=ProfileResponse)
def get_profile(user: User = Depends(require_user), db: Session = Depends(get_db)) -> ProfileResponse:
    profile = db.execute(select(Profile).where(Profile.user_id == user.id)).scalar_one_or_none()
    if profile is None:
        raise HTTPException(status_code=404, detail="Profile not found")
    return ProfileResponse(
        id=profile.id,
        user_id=profile.user_id,
        height_cm=profile.height_cm,
        weight_kg=profile.weight_kg,
        age=profile.age,
        sex=profile.sex,
        created_at=profile.created_at.isoformat(),
        updated_at=profile.updated_at.isoformat(),
    )


@app.put("/profile/me", response_model=ProfileResponse)
def upsert_profile(req: ProfileRequest, user: User = Depends(require_user), db: Session = Depends(get_db)) -> ProfileResponse:
    profile = db.execute(select(Profile).where(Profile.user_id == user.id)).scalar_one_or_none()
    if profile is None:
        profile = Profile(
            user_id=user.id,
            height_cm=req.height_cm,
            weight_kg=req.weight_kg,
            age=req.age,
            sex=req.sex,
            created_at=now(),
            updated_at=now(),
        )
        db.add(profile)
    else:
        profile.height_cm = req.height_cm
        profile.weight_kg = req.weight_kg
        profile.age = req.age
        profile.sex = req.sex
        profile.updated_at = now()
    db.commit()
    db.refresh(profile)
    return ProfileResponse(
        id=profile.id,
        user_id=profile.user_id,
        height_cm=profile.height_cm,
        weight_kg=profile.weight_kg,
        age=profile.age,
        sex=profile.sex,
        created_at=profile.created_at.isoformat(),
        updated_at=profile.updated_at.isoformat(),
    )


@app.post("/admin/ad_units/upsert")
def admin_upsert_ad_unit(
    req: AdUnitUpsert,
    x_admin_key: str | None = Header(default=None, alias="X-Admin-Key"),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    require_admin(x_admin_key)
    existing = db.execute(
        select(AdUnit).where(AdUnit.network == req.network, AdUnit.placement == req.placement)
    ).scalar_one_or_none()
    if existing is None:
        item = AdUnit(
            network=req.network,
            placement=req.placement,
            ad_unit_id=req.ad_unit_id,
            enabled=req.enabled,
            android_min_version=req.android_min_version,
            android_max_version=req.android_max_version,
            created_at=now(),
            updated_at=now(),
        )
        db.add(item)
        db.commit()
        return {"status": "created"}

    existing.ad_unit_id = req.ad_unit_id
    existing.enabled = req.enabled
    existing.android_min_version = req.android_min_version
    existing.android_max_version = req.android_max_version
    existing.updated_at = now()
    db.commit()
    return {"status": "updated"}


@app.get("/user_settings/me", response_model=UserSettingsResponse)
def get_user_settings(user: User = Depends(require_user), db: Session = Depends(get_db)) -> UserSettingsResponse:
    user_settings = db.execute(select(UserSettings).where(UserSettings.user_id == user.id)).scalar_one_or_none()
    if user_settings is None:
        raise HTTPException(status_code=404, detail="User settings not found")
    return UserSettingsResponse(
        id=user_settings.id,
        user_id=user_settings.user_id,
        calorie_mode=user_settings.calorie_mode,
        step_goal=user_settings.step_goal,
        calorie_goal_override=user_settings.calorie_goal_override,
        reminders_enabled=user_settings.reminders_enabled,
        updated_at=user_settings.updated_at.isoformat(),
    )


@app.put("/user_settings/me", response_model=UserSettingsResponse)
def upsert_user_settings(req: UserSettingsRequest, user: User = Depends(require_user), db: Session = Depends(get_db)) -> UserSettingsResponse:
    user_settings = db.execute(select(UserSettings).where(UserSettings.user_id == user.id)).scalar_one_or_none()
    if user_settings is None:
        user_settings = UserSettings(
            user_id=user.id,
            calorie_mode=req.calorie_mode,
            step_goal=req.step_goal,
            calorie_goal_override=req.calorie_goal_override,
            reminders_enabled=req.reminders_enabled,
            updated_at=now(),
        )
        db.add(user_settings)
    else:
        user_settings.calorie_mode = req.calorie_mode
        user_settings.step_goal = req.step_goal
        user_settings.calorie_goal_override = req.calorie_goal_override
        user_settings.reminders_enabled = req.reminders_enabled
        user_settings.updated_at = now()
    db.commit()
    db.refresh(user_settings)
    return UserSettingsResponse(
        id=user_settings.id,
        user_id=user_settings.user_id,
        calorie_mode=user_settings.calorie_mode,
        step_goal=user_settings.step_goal,
        calorie_goal_override=user_settings.calorie_goal_override,
        reminders_enabled=user_settings.reminders_enabled,
        updated_at=user_settings.updated_at.isoformat(),
    )


@app.post("/families", response_model=FamilyResponse)
def create_family(req: FamilyRequest, user: User = Depends(require_user), db: Session = Depends(get_db)) -> FamilyResponse:
    existing_family = db.execute(select(Family).where(Family.name == req.name)).scalar_one_or_none()
    if existing_family is not None:
        raise HTTPException(status_code=409, detail="Family with this name already exists")

    # Check if user is already in a family
    existing_member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if existing_member is not None:
        raise HTTPException(status_code=409, detail="User is already in a family")

    family = Family(
        name=req.name,
        admin_user_id=user.id,
        created_at=now(),
    )
    db.add(family)
    db.flush() # To get family.id

    member = FamilyMember(
        family_id=family.id,
        user_id=user.id,
        joined_at=now(),
    )
    db.add(member)
    db.commit()
    db.refresh(family)
    db.refresh(member) # For member.user to be loaded

    return FamilyResponse(
        id=family.id,
        name=family.name,
        admin_user_id=family.admin_user_id,
        created_at=family.created_at.isoformat(),
        members=[
            FamilyMemberResponse(
                user_id=member.user.id,
                login=member.user.login,
                joined_at=member.joined_at.isoformat(),
            )
            for member in family.members
        ],
    )


@app.get("/families/me", response_model=FamilyResponse)
def get_my_family(user: User = Depends(require_user), db: Session = Depends(get_db)) -> FamilyResponse:
    member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if member is None:
        raise HTTPException(status_code=404, detail="User is not in a family")

    family = db.get(Family, member.family_id)
    if family is None: # Should not happen if data is consistent
        raise HTTPException(status_code=404, detail="Family not found")

    return FamilyResponse(
        id=family.id,
        name=family.name,
        admin_user_id=family.admin_user_id,
        created_at=family.created_at.isoformat(),
        members=[
            FamilyMemberResponse(
                user_id=m.user.id,
                login=m.user.login,
                joined_at=m.joined_at.isoformat(),
            )
            for m in family.members
        ],
    )


@app.post("/families/leave")
def leave_family(user: User = Depends(require_user), db: Session = Depends(get_db)) -> dict[str, str]:
    # Check if user is in a family
    member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if member is None:
        raise HTTPException(status_code=404, detail="User is not in a family")

    family = db.get(Family, member.family_id)
    if family is None: # Should not happen if data is consistent
        raise HTTPException(status_code=404, detail="Family not found")

    # If user is the only member, delete the family
    if len(family.members) == 1:
        db.delete(family)
    else:
        # If user is admin, transfer admin rights to another member or disallow leaving
        if family.admin_user_id == user.id:
            other_members = [m for m in family.members if m.user_id != user.id]
            if len(other_members) > 0:
                family.admin_user_id = other_members[0].user_id
            else:
                raise HTTPException(status_code=400, detail="Cannot leave family as the sole admin. Delete the family instead.")

        db.delete(member)
    db.commit()
    return {"status": "left"}


@app.get("/families/me/members", response_model=List[FamilyMemberResponse])
def get_my_family_members(user: User = Depends(require_user), db: Session = Depends(get_db)) -> List[FamilyMemberResponse]:
    member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if member is None:
        raise HTTPException(status_code=404, detail="User is not in a family")

    family = db.get(Family, member.family_id)
    if family is None: # Should not happen if data is consistent
        raise HTTPException(status_code=404, detail="Family not found")

    return [
        FamilyMemberResponse(
            user_id=m.user.id,
            login=m.user.login,
            joined_at=m.joined_at.isoformat(),
        )
        for m in family.members
    ]

@app.post("/families/me/invite")
def invite_user(req: InviteUserRequest, user: User = Depends(require_user), db: Session = Depends(get_db)) -> dict[str, str]:
    # Check if current user is admin of a family
    family = db.execute(select(Family).where(Family.admin_user_id == user.id)).scalar_one_or_none()
    if family is None:
        raise HTTPException(status_code=403, detail="User is not an admin of any family")

    # Check if target user exists
    target_user = db.execute(select(User).where(User.login == req.login)).scalar_one_or_none()
    if target_user is None:
        raise HTTPException(status_code=404, detail="Target user not found")

    # Check if target user is already in a family
    existing_member = db.execute(select(FamilyMember).where(FamilyMember.user_id == target_user.id)).scalar_one_or_none()
    if existing_member is not None:
        raise HTTPException(status_code=409, detail="Target user is already in a family")

    # Check if target user is already invited (implicitly by adding to FamilyMember table)
    existing_invitation = db.execute(select(FamilyMember).where(FamilyMember.family_id == family.id, FamilyMember.user_id == target_user.id)).scalar_one_or_none()
    if existing_invitation is not None:
        raise HTTPException(status_code=409, detail="Target user already invited to this family")

    # Add target user as a family member
    member = FamilyMember(
        family_id=family.id,
        user_id=target_user.id,
        joined_at=now(),
    )
    db.add(member)
    db.commit()
    return {"status": "invited"}


@app.post("/families/join", response_model=FamilyResponse)
def join_family(req: JoinFamilyRequest, user: User = Depends(require_user), db: Session = Depends(get_db)) -> FamilyResponse:
    # Check if user is already in a family
    existing_member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if existing_member is not None:
        raise HTTPException(status_code=409, detail="User is already in a family")

    # Check if family exists
    family = db.execute(select(Family).where(Family.name == req.family_name)).scalar_one_or_none()
    if family is None:
        raise HTTPException(status_code=404, detail="Family not found")

    # Add user as a family member
    member = FamilyMember(
        family_id=family.id,
        user_id=user.id,
        joined_at=now(),
    )
    db.add(member)
    db.commit()
    db.refresh(family)
    db.refresh(member) # For member.user to be loaded

    return FamilyResponse(
        id=family.id,
        name=family.name,
        admin_user_id=family.admin_user_id,
        created_at=family.created_at.isoformat(),
        members=[
            FamilyMemberResponse(
                user_id=m.user.id,
                login=m.user.login,
                joined_at=m.joined_at.isoformat(),
            )
            for m in family.members
        ],
    )

