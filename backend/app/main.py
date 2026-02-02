import datetime as dt
from typing import List

from fastapi import Depends, FastAPI, Form, Header, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.templating import Jinja2Templates
from starlette.middleware.sessions import SessionMiddleware
from starlette.responses import HTMLResponse, RedirectResponse
from sqlalchemy import delete, select, update, func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.db import Base, engine, get_db
from app.models import AdUnit, AdminUser, Family, FamilyGoal, FamilyInvite, FamilyMember, Profile, RefreshSession, User, UserSettings, StepEntry, WaterEntry, WeightEntry, SmokeStatus, FoodEntry, TrainingEntry, BookEntry, XpEvent, UserAchievement, SyncQueue, AdminSettings, Announcement
from app.schemas import (
    AdUnitUpsert,
    AdsConfigResponse,
    FamilyMemberResponse,
    FamilyRequest,
    FamilyResponse,
    FamilyInviteResponse,
    InviteUserRequest,
    JoinFamilyRequest,
    FamilyGoalsResponse,
    FamilyGoalsUpsertRequest,
    LoginRequest,
    LogoutRequest,
    ProfileRequest,
    ProfileResponse,
    RefreshRequest,
    RegisterRequest,
    StepEntryResponse,
    StepUpsertRequest,
    WaterCreateRequest,
    WaterEntryResponse,
    WeightEntryResponse,
    WeightUpsertRequest,
    SmokeStatusRequest,
    SmokeStatusResponse,
    FoodCreateRequest,
    FoodEntryResponse,
    TrainingCreateRequest,
    TrainingEntryResponse,
    BookCreateRequest,
    BookEntryResponse,
    BookProgressRequest,
    XpEventCreateRequest,
    XpEventResponse,
    XpDailyAggregateResponse,
    XpTotalResponse,
    UserAchievementResponse,
    SyncBatchItem,
    SyncBatchRequest,
    SyncBatchResponse,
    AdminSettingsRequest,
    AdminSettingsResponse,
    AnnouncementReadResponse,
    AnnouncementResponse,
    AnnouncementRequest,
    PrivacyPolicyAcceptResponse,
    PrivacyPolicyResponse,
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


app = FastAPI(title="Alta API")

templates = Jinja2Templates(directory="app/templates")

app.add_middleware(
    SessionMiddleware,
    secret_key=settings.JWT_SECRET,
    same_site="lax",
    https_only=False,
)

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


def week_bounds(target: dt.date | None = None) -> tuple[int, int]:
    """Return (week_start_epoch_day, week_end_epoch_day) for Monday-Sunday (UTC)."""
    if target is None:
        target = dt.datetime.now(dt.timezone.utc).date()
    weekday = target.weekday()  # Monday=0
    start_date = target - dt.timedelta(days=weekday)
    end_date = start_date + dt.timedelta(days=6)
    epoch = dt.date(1970, 1, 1)
    return (start_date - epoch).days, (end_date - epoch).days


def require_family_and_members(user: User, db: Session) -> tuple[Family, list[FamilyMember]]:
    member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if member is None:
        raise HTTPException(status_code=404, detail="User is not in a family")
    family = db.get(Family, member.family_id)
    if family is None:
        raise HTTPException(status_code=404, detail="Family not found")
    db.refresh(family)
    members = family.members
    return family, members


def aggregate_family_progress(members: list[FamilyMember], week_start: int, week_end: int, db: Session):
    member_ids = [m.user_id for m in members]
    if not member_ids:
        return {
            "steps": 0,
            "trainings": 0,
            "water": 0,
            "per_member": {},
        }

    steps_rows = db.execute(
        select(StepEntry.user_id, func.sum(StepEntry.steps))
        .where(
            StepEntry.user_id.in_(member_ids),
            StepEntry.date_epoch_day >= week_start,
            StepEntry.date_epoch_day <= week_end,
        )
        .group_by(StepEntry.user_id)
    ).all()
    steps_map = {uid: steps or 0 for uid, steps in steps_rows}

    trainings_rows = db.execute(
        select(TrainingEntry.user_id, func.count())
        .where(
            TrainingEntry.user_id.in_(member_ids),
            TrainingEntry.date_epoch_day >= week_start,
            TrainingEntry.date_epoch_day <= week_end,
        )
        .group_by(TrainingEntry.user_id)
    ).all()
    trainings_map = {uid: cnt or 0 for uid, cnt in trainings_rows}

    water_rows = db.execute(
        select(WaterEntry.user_id, func.sum(WaterEntry.amount_ml))
        .where(
            WaterEntry.user_id.in_(member_ids),
            WaterEntry.date_epoch_day >= week_start,
            WaterEntry.date_epoch_day <= week_end,
        )
        .group_by(WaterEntry.user_id)
    ).all()
    water_map = {uid: ml or 0 for uid, ml in water_rows}

    per_member = {}
    for m in members:
        per_member[m.user_id] = {
            "login": m.user.login,
            "steps": steps_map.get(m.user_id, 0),
            "trainings": trainings_map.get(m.user_id, 0),
            "water": water_map.get(m.user_id, 0),
        }

    total_steps = sum(v["steps"] for v in per_member.values())
    total_trainings = sum(v["trainings"] for v in per_member.values())
    total_water = sum(v["water"] for v in per_member.values())

    return {
        "steps": total_steps,
        "trainings": total_trainings,
        "water": total_water,
        "per_member": per_member,
    }


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


def require_admin_session(request: Request, db: Session) -> AdminUser:
    admin_user_id = request.session.get("admin_user_id")
    if not admin_user_id:
        raise HTTPException(status_code=303, headers={"Location": "/admin/login"})
    admin_user = db.get(AdminUser, int(admin_user_id))
    if admin_user is None:
        request.session.clear()
        raise HTTPException(status_code=303, headers={"Location": "/admin/login"})
    return admin_user


def admin_guard(request: Request, db: Session) -> AdminUser | RedirectResponse:
    try:
        return require_admin_session(request, db)
    except HTTPException as e:
        if e.status_code == 303:
            return RedirectResponse(url=e.headers["Location"], status_code=303)
        raise


@app.get("/admin", response_class=HTMLResponse)
def admin_root(request: Request, db: Session = Depends(get_db)):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    return RedirectResponse(url="/admin/users", status_code=303)


@app.get("/admin/login", response_class=HTMLResponse)
def admin_login_get(request: Request):
    return templates.TemplateResponse(
        "login.html",
        {"request": request, "title": "Admin • Login", "error": None},
    )


@app.post("/admin/login")
def admin_login_post(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db),
):
    admin_user = db.execute(select(AdminUser).where(AdminUser.username == username)).scalar_one_or_none()
    if admin_user is None or not verify_password(password, admin_user.password_hash):
        return templates.TemplateResponse(
            "login.html",
            {"request": request, "title": "Admin • Login", "error": "Неверный логин или пароль"},
            status_code=400,
        )
    request.session["admin_user_id"] = admin_user.id
    return RedirectResponse(url="/admin/users", status_code=303)


@app.get("/admin/register", response_class=HTMLResponse)
def admin_register_get(request: Request):
    return templates.TemplateResponse(
        "register.html",
        {"request": request, "title": "Admin • Register", "error": None},
    )


@app.post("/admin/register")
def admin_register_post(
    request: Request,
    username: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db),
):
    any_admin = db.execute(select(AdminUser.id).limit(1)).first() is not None
    if any_admin:
        return templates.TemplateResponse(
            "register.html",
            {
                "request": request,
                "title": "Admin • Register",
                "error": "Регистрация отключена. Используй существующий админ-аккаунт.",
            },
            status_code=403,
        )
    existing = db.execute(select(AdminUser).where(AdminUser.username == username)).scalar_one_or_none()
    if existing is not None:
        return templates.TemplateResponse(
            "register.html",
            {"request": request, "title": "Admin • Register", "error": "Пользователь уже существует"},
            status_code=400,
        )
    admin_user = AdminUser(username=username, password_hash=hash_password(password), created_at=now())
    db.add(admin_user)
    db.commit()
    db.refresh(admin_user)
    request.session["admin_user_id"] = admin_user.id
    return RedirectResponse(url="/admin/users", status_code=303)


@app.post("/admin/logout")
def admin_logout(request: Request):
    request.session.clear()
    return RedirectResponse(url="/admin/integration", status_code=303)


@app.get("/admin/settings", response_class=HTMLResponse)
def admin_settings_page(request: Request, db: Session = Depends(get_db)):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    admin_user = guard
    return templates.TemplateResponse(
        "admin_settings_new.html",
        {
            "request": request,
            "title": "Admin • Settings",
            "admin_user": admin_user,
        },
    )


@app.get("/admin/users", response_class=HTMLResponse)
def admin_users(request: Request, db: Session = Depends(get_db)):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    admin_user = guard

    users = db.execute(select(User).order_by(User.id.desc())).scalars().all()
    profiles = {p.user_id: p for p in db.execute(select(Profile)).scalars().all()}
    settings_map = {s.user_id: s for s in db.execute(select(UserSettings)).scalars().all()}

    family_name_by_user: dict[int, str] = {}
    members = db.execute(select(FamilyMember)).scalars().all()
    if members:
        family_ids = {m.family_id for m in members}
        families = {f.id: f for f in db.execute(select(Family).where(Family.id.in_(family_ids))).scalars().all()}
        for m in members:
            fam = families.get(m.family_id)
            if fam is not None:
                family_name_by_user[m.user_id] = fam.name

    rows = []
    for u in users:
        rows.append(
            {
                "user": u,
                "profile": profiles.get(u.id),
                "settings": settings_map.get(u.id),
                "family_name": family_name_by_user.get(u.id),
            }
        )

    return templates.TemplateResponse(
        "users.html",
        {
            "request": request,
            "title": "Admin • Users",
            "admin_user": admin_user,
            "rows": rows,
        },
    )


@app.get("/admin/ads", response_class=HTMLResponse)
def admin_ads(request: Request, db: Session = Depends(get_db)):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    admin_user = guard
    ads = db.execute(select(AdUnit).order_by(AdUnit.network.asc(), AdUnit.placement.asc())).scalars().all()
    settings_row = db.execute(select(AdminSettings)).scalar_one_or_none()
    return templates.TemplateResponse(
        "ads.html",
        {
            "request": request,
            "title": "Admin • Ads",
            "admin_user": admin_user,
            "ads": ads,
            "settings": settings_row,
        },
    )


@app.post("/admin/ads/appodeal")
def admin_ads_appodeal(
    request: Request,
    appodeal_app_key: str | None = Form(default=None),
    appodeal_enabled: str | None = Form(default=None),
    appodeal_banner_enabled: str | None = Form(default=None),
    appodeal_interstitial_enabled: str | None = Form(default=None),
    appodeal_rewarded_enabled: str | None = Form(default=None),
    db: Session = Depends(get_db),
):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard

    enabled_bool = appodeal_enabled == "on"
    banner_bool = appodeal_banner_enabled == "on"
    inter_bool = appodeal_interstitial_enabled == "on"
    rewarded_bool = appodeal_rewarded_enabled == "on"

    row = db.execute(select(AdminSettings)).scalar_one_or_none()
    if row is None:
        row = AdminSettings(
            openrouter_api_key=None,
            openrouter_model=None,
            appodeal_app_key=(appodeal_app_key or None),
            appodeal_enabled=enabled_bool,
            appodeal_banner_enabled=banner_bool,
            appodeal_interstitial_enabled=inter_bool,
            appodeal_rewarded_enabled=rewarded_bool,
            updated_at=now(),
        )
        db.add(row)
    else:
        row.appodeal_app_key = (appodeal_app_key or None)
        row.appodeal_enabled = enabled_bool
        row.appodeal_banner_enabled = banner_bool
        row.appodeal_interstitial_enabled = inter_bool
        row.appodeal_rewarded_enabled = rewarded_bool
        row.updated_at = now()

    db.commit()
    return RedirectResponse(url="/admin/ads", status_code=303)


@app.post("/admin/ads/upsert")
def admin_ads_upsert(
    request: Request,
    id: int | None = Form(default=None),
    network: str = Form(...),
    placement: str = Form(...),
    ad_unit_id: str = Form(...),
    enabled: str | None = Form(default=None),
    android_min_version: str | None = Form(default=None),
    android_max_version: str | None = Form(default=None),
    db: Session = Depends(get_db),
):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard

    enabled_bool = enabled == "on"
    min_v = int(android_min_version) if android_min_version else None
    max_v = int(android_max_version) if android_max_version else None

    if id:
        item = db.get(AdUnit, int(id))
        if item is None:
            raise HTTPException(status_code=404, detail="Not found")
        item.network = network
        item.placement = placement
        item.ad_unit_id = ad_unit_id
        item.enabled = enabled_bool
        item.android_min_version = min_v
        item.android_max_version = max_v
        item.updated_at = now()
        db.commit()
        return RedirectResponse(url="/admin/ads", status_code=303)

    existing = db.execute(select(AdUnit).where(AdUnit.network == network, AdUnit.placement == placement)).scalar_one_or_none()
    if existing is not None:
        existing.ad_unit_id = ad_unit_id
        existing.enabled = enabled_bool
        existing.android_min_version = min_v
        existing.android_max_version = max_v
        existing.updated_at = now()
        db.commit()
        return RedirectResponse(url="/admin/ads", status_code=303)

    item = AdUnit(
        network=network,
        placement=placement,
        ad_unit_id=ad_unit_id,
        enabled=enabled_bool,
        android_min_version=min_v,
        android_max_version=max_v,
        created_at=now(),
        updated_at=now(),
    )
    db.add(item)
    db.commit()
    return RedirectResponse(url="/admin/ads", status_code=303)


@app.post("/admin/ads/delete")
def admin_ads_delete(
    request: Request,
    id: int = Form(...),
    db: Session = Depends(get_db),
):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    item = db.get(AdUnit, int(id))
    if item is not None:
        db.delete(item)
        db.commit()
    return RedirectResponse(url="/admin/ads", status_code=303)


@app.get("/admin/integration", response_class=HTMLResponse)
def admin_integration(request: Request, db: Session = Depends(get_db)):
    guard = admin_guard(request, db)
    if isinstance(guard, RedirectResponse):
        return guard
    admin_user = guard
    ads = db.execute(select(AdUnit).where(AdUnit.enabled == True)).scalars().all()  # noqa: E712
    placements = {a.placement: a.ad_unit_id for a in ads}
    return templates.TemplateResponse(
        "integration.html",
        {
            "request": request,
            "title": "Admin • Integration",
            "admin_user": admin_user,
            "placements": placements,
        },
    )


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

    settings_row = db.execute(select(AdminSettings)).scalar_one_or_none()
    return AdsConfigResponse(
        network=network,
        units={u.placement: u.ad_unit_id for u in filtered},
        appodeal_app_key=settings_row.appodeal_app_key if settings_row is not None else None,
        appodeal_enabled=settings_row.appodeal_enabled if settings_row is not None else False,
        appodeal_banner_enabled=settings_row.appodeal_banner_enabled if settings_row is not None else True,
        appodeal_interstitial_enabled=settings_row.appodeal_interstitial_enabled if settings_row is not None else True,
        appodeal_rewarded_enabled=settings_row.appodeal_rewarded_enabled if settings_row is not None else True,
    )


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
        target_weight_kg=user_settings.target_weight_kg,
        reminders_enabled=user_settings.reminders_enabled,
        privacy_policy_accepted_at=user_settings.privacy_policy_accepted_at.isoformat() if user_settings.privacy_policy_accepted_at else None,
        privacy_policy_accepted_policy_updated_at=user_settings.privacy_policy_accepted_policy_updated_at.isoformat() if user_settings.privacy_policy_accepted_policy_updated_at else None,
        announcement_read_at=user_settings.announcement_read_at.isoformat() if user_settings.announcement_read_at else None,
        announcement_read_announcement_updated_at=user_settings.announcement_read_announcement_updated_at.isoformat() if user_settings.announcement_read_announcement_updated_at else None,
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
            target_weight_kg=req.target_weight_kg,
            reminders_enabled=req.reminders_enabled,
            updated_at=now(),
        )
        db.add(user_settings)
    else:
        user_settings.calorie_mode = req.calorie_mode
        user_settings.step_goal = req.step_goal
        user_settings.calorie_goal_override = req.calorie_goal_override
        user_settings.target_weight_kg = req.target_weight_kg
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
        target_weight_kg=user_settings.target_weight_kg,
        reminders_enabled=user_settings.reminders_enabled,
        privacy_policy_accepted_at=user_settings.privacy_policy_accepted_at.isoformat() if user_settings.privacy_policy_accepted_at else None,
        privacy_policy_accepted_policy_updated_at=user_settings.privacy_policy_accepted_policy_updated_at.isoformat() if user_settings.privacy_policy_accepted_policy_updated_at else None,
        updated_at=user_settings.updated_at.isoformat(),
    )


@app.get("/privacy_policy", response_model=PrivacyPolicyResponse)
def get_privacy_policy(db: Session = Depends(get_db)) -> PrivacyPolicyResponse:
    row = db.execute(select(AdminSettings)).scalar_one_or_none()
    if row is None or not row.privacy_policy_text or not row.privacy_policy_updated_at:
        raise HTTPException(status_code=404, detail="Privacy policy not configured")
    return PrivacyPolicyResponse(text=row.privacy_policy_text, updated_at=row.privacy_policy_updated_at.isoformat())


@app.post("/privacy_policy/accept", response_model=PrivacyPolicyAcceptResponse)
def accept_privacy_policy(user: User = Depends(require_user), db: Session = Depends(get_db)) -> PrivacyPolicyAcceptResponse:
    policy = db.execute(select(AdminSettings)).scalar_one_or_none()
    if policy is None or not policy.privacy_policy_updated_at:
        raise HTTPException(status_code=404, detail="Privacy policy not configured")

    user_settings = db.execute(select(UserSettings).where(UserSettings.user_id == user.id)).scalar_one_or_none()
    if user_settings is None:
        raise HTTPException(status_code=404, detail="User settings not found")

    accepted_at = now()
    user_settings.privacy_policy_accepted_at = accepted_at
    user_settings.privacy_policy_accepted_policy_updated_at = policy.privacy_policy_updated_at
    user_settings.updated_at = now()
    db.commit()
    return PrivacyPolicyAcceptResponse(
        accepted_at=accepted_at.isoformat(),
        policy_updated_at=policy.privacy_policy_updated_at.isoformat(),
    )


@app.get("/announcement", response_model=AnnouncementResponse)
def get_announcement(db: Session = Depends(get_db)) -> AnnouncementResponse:
    row = db.execute(select(AdminSettings)).scalar_one_or_none()
    if (
        row is None
        or not row.announcement_enabled
        or not row.announcement_text
        or not row.announcement_updated_at
    ):
        raise HTTPException(status_code=404, detail="Announcement not configured")

    return AnnouncementResponse(
        text=row.announcement_text,
        updated_at=row.announcement_updated_at.isoformat(),
        button_enabled=row.announcement_button_enabled,
        button_text=row.announcement_button_text,
        button_url=row.announcement_button_url,
    )


@app.post("/announcement/read", response_model=AnnouncementReadResponse)
def read_announcement(user: User = Depends(require_user), db: Session = Depends(get_db)) -> AnnouncementReadResponse:
    announcement = db.execute(select(AdminSettings)).scalar_one_or_none()
    if announcement is None or not announcement.announcement_updated_at:
        raise HTTPException(status_code=404, detail="Announcement not configured")

    user_settings = db.execute(select(UserSettings).where(UserSettings.user_id == user.id)).scalar_one_or_none()
    if user_settings is None:
        raise HTTPException(status_code=404, detail="User settings not found")

    read_at = now()
    user_settings.announcement_read_at = read_at
    user_settings.announcement_read_announcement_updated_at = announcement.announcement_updated_at
    user_settings.updated_at = now()
    db.commit()

    return AnnouncementReadResponse(
        read_at=read_at.isoformat(),
        announcement_updated_at=announcement.announcement_updated_at.isoformat(),
    )


# Admin announcements (news) management
@app.get("/admin/announcements", response_model=List[AnnouncementResponse])
def list_announcements(db: Session = Depends(get_db)) -> List[AnnouncementResponse]:
    rows = db.execute(select(Announcement).order_by(Announcement.created_at.desc())).scalars().all()
    return [
        AnnouncementResponse(
            id=row.id,
            title=row.title,
            text=row.text,
            button_enabled=row.button_enabled,
            button_text=row.button_text,
            button_url=row.button_url,
            is_active=row.is_active,
            created_at=row.created_at.isoformat(),
            updated_at=row.updated_at.isoformat(),
        )
        for row in rows
    ]


@app.post("/admin/announcements", response_model=AnnouncementResponse)
def create_announcement(req: AnnouncementRequest, db: Session = Depends(get_db)) -> AnnouncementResponse:
    row = Announcement(
        title=req.title,
        text=req.text,
        button_enabled=req.button_enabled,
        button_text=req.button_text,
        button_url=req.button_url,
        is_active=req.is_active,
        created_at=now(),
        updated_at=now(),
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return AnnouncementResponse(
        id=row.id,
        title=row.title,
        text=row.text,
        button_enabled=row.button_enabled,
        button_text=row.button_text,
        button_url=row.button_url,
        is_active=row.is_active,
        created_at=row.created_at.isoformat(),
        updated_at=row.updated_at.isoformat(),
    )


@app.put("/admin/announcements/{announcement_id}", response_model=AnnouncementResponse)
def update_announcement(announcement_id: int, req: AnnouncementRequest, db: Session = Depends(get_db)) -> AnnouncementResponse:
    row = db.execute(select(Announcement).where(Announcement.id == announcement_id)).scalar_one_or_none()
    if row is None:
        raise HTTPException(status_code=404, detail="Announcement not found")
    row.title = req.title
    row.text = req.text
    row.button_enabled = req.button_enabled
    row.button_text = req.button_text
    row.button_url = req.button_url
    row.is_active = req.is_active
    row.updated_at = now()
    db.commit()
    db.refresh(row)
    return AnnouncementResponse(
        id=row.id,
        title=row.title,
        text=row.text,
        button_enabled=row.button_enabled,
        button_text=row.button_text,
        button_url=row.button_url,
        is_active=row.is_active,
        created_at=row.created_at.isoformat(),
        updated_at=row.updated_at.isoformat(),
    )


@app.delete("/admin/announcements/{announcement_id}")
def delete_announcement(announcement_id: int, db: Session = Depends(get_db)):
    row = db.execute(select(Announcement).where(Announcement.id == announcement_id)).scalar_one_or_none()
    if row is None:
        raise HTTPException(status_code=404, detail="Announcement not found")
    db.delete(row)
    db.commit()
    return {"detail": "Announcement deleted"}


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


@app.get("/families/me/goals", response_model=FamilyGoalsResponse)
def get_family_goals(user: User = Depends(require_user), db: Session = Depends(get_db)) -> FamilyGoalsResponse:
    family, members = require_family_and_members(user, db)
    week_start, week_end = week_bounds()

    goal = db.execute(
        select(FamilyGoal).where(
            FamilyGoal.family_id == family.id,
            FamilyGoal.week_start_epoch_day == week_start,
        )
    ).scalar_one_or_none()

    steps_goal = goal.steps_goal if goal else 70000
    trainings_goal = goal.trainings_goal if goal else 6
    water_goal_ml = goal.water_goal_ml if goal else 21000

    agg = aggregate_family_progress(members, week_start, week_end, db)

    contributions = [
        FamilyGoalContribution(
            user_id=uid,
            login=data["login"],
            steps=data["steps"],
            trainings=data["trainings"],
            water_ml=data["water"],
        )
        for uid, data in agg["per_member"].items()
    ]

    return FamilyGoalsResponse(
        week_start_epoch_day=week_start,
        week_end_epoch_day=week_end,
        steps_goal=steps_goal,
        steps_progress=agg["steps"],
        trainings_goal=trainings_goal,
        trainings_progress=agg["trainings"],
        water_goal_ml=water_goal_ml,
        water_progress_ml=agg["water"],
        contributions=contributions,
    )


@app.put("/families/me/goals", response_model=FamilyGoalsResponse)
def upsert_family_goals(
    req: FamilyGoalsUpsertRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> FamilyGoalsResponse:
    family, members = require_family_and_members(user, db)
    if family.admin_user_id != user.id:
        raise HTTPException(status_code=403, detail="Only family admin can update goals")

    week_start, week_end = week_bounds()

    goal = db.execute(
        select(FamilyGoal).where(
            FamilyGoal.family_id == family.id,
            FamilyGoal.week_start_epoch_day == week_start,
        )
    ).scalar_one_or_none()

    if goal is None:
        goal = FamilyGoal(
            family_id=family.id,
            week_start_epoch_day=week_start,
            steps_goal=req.steps_goal,
            trainings_goal=req.trainings_goal,
            water_goal_ml=req.water_goal_ml,
            created_at=now(),
        )
        db.add(goal)
    else:
        goal.steps_goal = req.steps_goal
        goal.trainings_goal = req.trainings_goal
        goal.water_goal_ml = req.water_goal_ml
    db.commit()

    agg = aggregate_family_progress(members, week_start, week_end, db)
    contributions = [
        FamilyGoalContribution(
            user_id=uid,
            login=data["login"],
            steps=data["steps"],
            trainings=data["trainings"],
            water_ml=data["water"],
        )
        for uid, data in agg["per_member"].items()
    ]

    return FamilyGoalsResponse(
        week_start_epoch_day=week_start,
        week_end_epoch_day=week_end,
        steps_goal=goal.steps_goal,
        steps_progress=agg["steps"],
        trainings_goal=goal.trainings_goal,
        trainings_progress=agg["trainings"],
        water_goal_ml=goal.water_goal_ml,
        water_progress_ml=agg["water"],
        contributions=contributions,
    )

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

    existing_invite = db.execute(
        select(FamilyInvite).where(
            FamilyInvite.family_id == family.id,
            FamilyInvite.invited_user_id == target_user.id,
        )
    ).scalar_one_or_none()
    if existing_invite is not None:
        raise HTTPException(status_code=409, detail="Target user already invited to this family")

    invite = FamilyInvite(
        family_id=family.id,
        invited_user_id=target_user.id,
        invited_by_user_id=user.id,
        created_at=now(),
    )
    db.add(invite)
    db.commit()
    return {"status": "invited"}


@app.get("/families/invites", response_model=List[FamilyInviteResponse])
def get_family_invites(user: User = Depends(require_user), db: Session = Depends(get_db)) -> List[FamilyInviteResponse]:
    rows = (
        db.execute(
            select(FamilyInvite)
            .where(FamilyInvite.invited_user_id == user.id)
            .order_by(FamilyInvite.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        FamilyInviteResponse(
            id=r.id,
            family_id=r.family_id,
            family_name=r.family.name,
            invited_by_user_id=r.invited_by_user_id,
            invited_by_login=r.invited_by_user.login,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/families/invites/{invite_id}/accept", response_model=FamilyResponse)
def accept_family_invite(invite_id: int, user: User = Depends(require_user), db: Session = Depends(get_db)) -> FamilyResponse:
    # Cannot accept if already in a family
    existing_member = db.execute(select(FamilyMember).where(FamilyMember.user_id == user.id)).scalar_one_or_none()
    if existing_member is not None:
        raise HTTPException(status_code=409, detail="User is already in a family")

    invite = db.get(FamilyInvite, invite_id)
    if invite is None or invite.invited_user_id != user.id:
        raise HTTPException(status_code=404, detail="Invite not found")

    family = db.get(Family, invite.family_id)
    if family is None:
        raise HTTPException(status_code=404, detail="Family not found")

    # Create membership
    member = FamilyMember(
        family_id=family.id,
        user_id=user.id,
        joined_at=now(),
    )
    db.add(member)

    # Delete all invites for this user (only 1 family allowed)
    db.execute(delete(FamilyInvite).where(FamilyInvite.invited_user_id == user.id))
    db.commit()
    db.refresh(family)

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


@app.post("/families/invites/{invite_id}/decline")
def decline_family_invite(invite_id: int, user: User = Depends(require_user), db: Session = Depends(get_db)) -> dict[str, str]:
    invite = db.get(FamilyInvite, invite_id)
    if invite is None or invite.invited_user_id != user.id:
        raise HTTPException(status_code=404, detail="Invite not found")

    db.delete(invite)
    db.commit()
    return {"status": "declined"}


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


@app.get("/steps/me", response_model=List[StepEntryResponse])
def get_steps_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[StepEntryResponse]:
    rows = (
        db.execute(
            select(StepEntry)
            .where(
                StepEntry.user_id == user.id,
                StepEntry.date_epoch_day >= start,
                StepEntry.date_epoch_day <= end,
            )
            .order_by(StepEntry.date_epoch_day.asc())
        )
        .scalars()
        .all()
    )
    return [
        StepEntryResponse(
            date_epoch_day=r.date_epoch_day,
            steps=r.steps,
            updated_at=r.updated_at.isoformat(),
        )
        for r in rows
    ]


@app.put("/steps/me", response_model=StepEntryResponse)
def upsert_steps_me(
    req: StepUpsertRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> StepEntryResponse:
    row = (
        db.execute(
            select(StepEntry).where(
                StepEntry.user_id == user.id,
                StepEntry.date_epoch_day == req.date_epoch_day,
            )
        )
        .scalars()
        .one_or_none()
    )

    if row is None:
        row = StepEntry(
            user_id=user.id,
            date_epoch_day=req.date_epoch_day,
            steps=req.steps,
            updated_at=now(),
        )
        db.add(row)
    else:
        row.steps = req.steps
        row.updated_at = now()

    db.commit()
    db.refresh(row)
    return StepEntryResponse(
        date_epoch_day=row.date_epoch_day,
        steps=row.steps,
        updated_at=row.updated_at.isoformat(),
    )


@app.get("/water/me", response_model=List[WaterEntryResponse])
def get_water_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[WaterEntryResponse]:
    rows = (
        db.execute(
            select(WaterEntry)
            .where(
                WaterEntry.user_id == user.id,
                WaterEntry.date_epoch_day >= start,
                WaterEntry.date_epoch_day <= end,
            )
            .order_by(WaterEntry.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        WaterEntryResponse(
            id=r.id,
            date_epoch_day=r.date_epoch_day,
            amount_ml=r.amount_ml,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/water/me", response_model=WaterEntryResponse)
def create_water_me(
    req: WaterCreateRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> WaterEntryResponse:
    row = WaterEntry(
        user_id=user.id,
        date_epoch_day=req.date_epoch_day,
        amount_ml=req.amount_ml,
        created_at=now(),
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return WaterEntryResponse(
        id=row.id,
        date_epoch_day=row.date_epoch_day,
        amount_ml=row.amount_ml,
        created_at=row.created_at.isoformat(),
    )


@app.delete("/water/me/{entry_id}")
def delete_water_me(
    entry_id: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    row = db.get(WaterEntry, entry_id)
    if row is None or row.user_id != user.id:
        raise HTTPException(status_code=404, detail="Not found")
    db.delete(row)
    db.commit()
    return {"status": "deleted"}


@app.get("/books/me", response_model=List[BookEntryResponse])
def get_books_me(
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[BookEntryResponse]:
    rows = (
        db.execute(
            select(BookEntry)
            .where(BookEntry.user_id == user.id)
            .order_by(BookEntry.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        BookEntryResponse(
            id=r.id,
            title=r.title,
            author=r.author,
            total_pages=r.total_pages,
            pages_read=r.pages_read,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/books/me", response_model=BookEntryResponse)
def create_book_me(
    req: BookCreateRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> BookEntryResponse:
    row = BookEntry(
        user_id=user.id,
        title=req.title,
        author=req.author,
        total_pages=req.total_pages,
        pages_read=0,
        created_at=now(),
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return BookEntryResponse(
        id=row.id,
        title=row.title,
        author=row.author,
        total_pages=row.total_pages,
        pages_read=row.pages_read,
        created_at=row.created_at.isoformat(),
    )


@app.put("/books/me/{entry_id}", response_model=BookEntryResponse)
def update_book_progress_me(
    entry_id: int,
    req: BookProgressRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> BookEntryResponse:
    row = db.get(BookEntry, entry_id)
    if row is None or row.user_id != user.id:
        raise HTTPException(status_code=404, detail="Not found")
    row.pages_read = req.pages_read
    db.commit()
    db.refresh(row)
    return BookEntryResponse(
        id=row.id,
        title=row.title,
        author=row.author,
        total_pages=row.total_pages,
        pages_read=row.pages_read,
        created_at=row.created_at.isoformat(),
    )


@app.delete("/books/me/{entry_id}")
def delete_book_me(
    entry_id: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    row = db.get(BookEntry, entry_id)
    if row is None or row.user_id != user.id:
        raise HTTPException(status_code=404, detail="Not found")
    db.delete(row)
    db.commit()
    return {"status": "deleted"}


@app.get("/training/me", response_model=List[TrainingEntryResponse])
def get_training_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[TrainingEntryResponse]:
    rows = (
        db.execute(
            select(TrainingEntry)
            .where(
                TrainingEntry.user_id == user.id,
                TrainingEntry.date_epoch_day >= start,
                TrainingEntry.date_epoch_day <= end,
            )
            .order_by(TrainingEntry.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        TrainingEntryResponse(
            id=r.id,
            date_epoch_day=r.date_epoch_day,
            title=r.title,
            description=r.description,
            calories_burned=r.calories_burned,
            duration_minutes=r.duration_minutes,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/training/me", response_model=TrainingEntryResponse)
def create_training_me(
    req: TrainingCreateRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> TrainingEntryResponse:
    row = TrainingEntry(
        user_id=user.id,
        date_epoch_day=req.date_epoch_day,
        title=req.title,
        description=req.description,
        calories_burned=req.calories_burned,
        duration_minutes=req.duration_minutes,
        created_at=now(),
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return TrainingEntryResponse(
        id=row.id,
        date_epoch_day=row.date_epoch_day,
        title=row.title,
        description=row.description,
        calories_burned=row.calories_burned,
        duration_minutes=row.duration_minutes,
        created_at=row.created_at.isoformat(),
    )


@app.delete("/training/me/{entry_id}")
def delete_training_me(
    entry_id: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    row = db.get(TrainingEntry, entry_id)
    if row is None or row.user_id != user.id:
        raise HTTPException(status_code=404, detail="Not found")
    db.delete(row)
    db.commit()
    return {"status": "deleted"}


@app.delete("/water/me")
def clear_water_day_me(
    date_epoch_day: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    db.execute(
        delete(WaterEntry).where(
            WaterEntry.user_id == user.id,
            WaterEntry.date_epoch_day == date_epoch_day,
        )
    )
    db.commit()
    return {"status": "cleared"}


@app.get("/weight/me", response_model=List[WeightEntryResponse])
def get_weight_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[WeightEntryResponse]:
    rows = (
        db.execute(
            select(WeightEntry)
            .where(
                WeightEntry.user_id == user.id,
                WeightEntry.date_epoch_day >= start,
                WeightEntry.date_epoch_day <= end,
            )
            .order_by(WeightEntry.date_epoch_day.asc())
        )
        .scalars()
        .all()
    )
    return [
        WeightEntryResponse(
            date_epoch_day=r.date_epoch_day,
            weight_kg=r.weight_kg,
            updated_at=r.updated_at.isoformat(),
        )
        for r in rows
    ]


@app.put("/weight/me", response_model=WeightEntryResponse)
def upsert_weight_me(
    req: WeightUpsertRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> WeightEntryResponse:
    row = (
        db.execute(
            select(WeightEntry).where(
                WeightEntry.user_id == user.id,
                WeightEntry.date_epoch_day == req.date_epoch_day,
            )
        )
        .scalars()
        .one_or_none()
    )

    if row is None:
        row = WeightEntry(
            user_id=user.id,
            date_epoch_day=req.date_epoch_day,
            weight_kg=req.weight_kg,
            created_at=now(),
            updated_at=now(),
        )
        db.add(row)
    else:
        row.weight_kg = req.weight_kg
        row.updated_at = now()

    db.commit()
    db.refresh(row)
    return WeightEntryResponse(
        date_epoch_day=row.date_epoch_day,
        weight_kg=row.weight_kg,
        updated_at=row.updated_at.isoformat(),
    )


@app.get("/smoke/me", response_model=SmokeStatusResponse)
def get_smoke_me(
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> SmokeStatusResponse:
    row = db.execute(select(SmokeStatus).where(SmokeStatus.user_id == user.id)).scalar_one_or_none()
    if row is None:
        row = SmokeStatus(
            user_id=user.id,
            started_at=now(),
            is_active=True,
            pack_price=0.0,
            packs_per_day=0.0,
            updated_at=now(),
        )
        db.add(row)
        db.commit()
        db.refresh(row)
    return SmokeStatusResponse(
        started_at=row.started_at.isoformat(),
        is_active=row.is_active,
        pack_price=row.pack_price,
        packs_per_day=row.packs_per_day,
        updated_at=row.updated_at.isoformat(),
    )


@app.get("/food/me", response_model=List[FoodEntryResponse])
def get_food_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[FoodEntryResponse]:
    rows = (
        db.execute(
            select(FoodEntry)
            .where(
                FoodEntry.user_id == user.id,
                FoodEntry.date_epoch_day >= start,
                FoodEntry.date_epoch_day <= end,
            )
            .order_by(FoodEntry.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        FoodEntryResponse(
            id=r.id,
            date_epoch_day=r.date_epoch_day,
            title=r.title,
            calories=r.calories,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/food/me", response_model=FoodEntryResponse)
def create_food_me(
    req: FoodCreateRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> FoodEntryResponse:
    row = FoodEntry(
        user_id=user.id,
        date_epoch_day=req.date_epoch_day,
        title=req.title,
        calories=req.calories,
        created_at=now(),
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return FoodEntryResponse(
        id=row.id,
        date_epoch_day=row.date_epoch_day,
        title=row.title,
        calories=row.calories,
        created_at=row.created_at.isoformat(),
    )


@app.delete("/food/me/{entry_id}")
def delete_food_me(
    entry_id: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> dict[str, str]:
    row = db.get(FoodEntry, entry_id)
    if row is None or row.user_id != user.id:
        raise HTTPException(status_code=404, detail="Not found")
    db.delete(row)
    db.commit()
    return {"status": "deleted"}


@app.put("/smoke/me", response_model=SmokeStatusResponse)
def upsert_smoke_me(
    req: SmokeStatusRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> SmokeStatusResponse:
    started_at = dt.datetime.fromisoformat(req.started_at.replace("Z", "+00:00"))
    row = db.execute(select(SmokeStatus).where(SmokeStatus.user_id == user.id)).scalar_one_or_none()
    if row is None:
        row = SmokeStatus(
            user_id=user.id,
            started_at=started_at,
            is_active=req.is_active,
            pack_price=req.pack_price,
            packs_per_day=req.packs_per_day,
            updated_at=now(),
        )
        db.add(row)
    else:
        row.started_at = started_at
        row.is_active = req.is_active
        row.pack_price = req.pack_price
        row.packs_per_day = req.packs_per_day
        row.updated_at = now()

    db.commit()
    db.refresh(row)
    return SmokeStatusResponse(
        started_at=row.started_at.isoformat(),
        is_active=row.is_active,
        pack_price=row.pack_price,
        packs_per_day=row.packs_per_day,
        updated_at=row.updated_at.isoformat(),
    )


@app.post("/xp/me", response_model=XpEventResponse)
def create_xp_event_me(
    req: XpEventCreateRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> XpEventResponse:
    row = XpEvent(
        user_id=user.id,
        date_epoch_day=req.date_epoch_day,
        type=req.type,
        points=req.points,
        note=req.note,
        created_at=now(),
    )
    db.add(row)
    try:
        db.commit()
        db.refresh(row)
    except IntegrityError:
        db.rollback()
        existing = db.execute(
            select(XpEvent).where(
                XpEvent.user_id == user.id,
                XpEvent.date_epoch_day == req.date_epoch_day,
                XpEvent.type == req.type,
                XpEvent.note == req.note,
            )
        ).scalar_one_or_none()
        if existing is None:
            raise
        row = existing
    return XpEventResponse(
        id=row.id,
        date_epoch_day=row.date_epoch_day,
        type=row.type,
        points=row.points,
        note=row.note,
        created_at=row.created_at.isoformat(),
    )


@app.get("/xp/me/daily", response_model=List[XpDailyAggregateResponse])
def get_xp_daily_me(
    start: int,
    end: int,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[XpDailyAggregateResponse]:
    rows = (
        db.execute(
            select(
                XpEvent.date_epoch_day,
                func.sum(XpEvent.points).label("total_points"),
            )
            .where(
                XpEvent.user_id == user.id,
                XpEvent.date_epoch_day >= start,
                XpEvent.date_epoch_day <= end,
            )
            .group_by(XpEvent.date_epoch_day)
            .order_by(XpEvent.date_epoch_day)
        )
        .all()
    )
    return [
        XpDailyAggregateResponse(
            date_epoch_day=row.date_epoch_day,
            total_points=int(row.total_points) if row.total_points else 0,
        )
        for row in rows
    ]


@app.get("/xp/me/total", response_model=XpTotalResponse)
def get_xp_total_me(
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> XpTotalResponse:
    total = (
        db.execute(select(func.coalesce(func.sum(XpEvent.points), 0)).where(XpEvent.user_id == user.id))
        .scalar()
    )
    total_points = int(total) if total else 0
    # Simple level formula: level = floor(sqrt(total_points / 100))
    level = int((total_points // 100) ** 0.5) if total_points > 0 else 0
    return XpTotalResponse(total_points=total_points, level=level)


@app.get("/achievements/me", response_model=List[UserAchievementResponse])
def get_achievements_me(
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> List[UserAchievementResponse]:
    rows = (
        db.execute(
            select(UserAchievement)
            .where(UserAchievement.user_id == user.id)
            .order_by(UserAchievement.created_at.desc())
        )
        .scalars()
        .all()
    )
    return [
        UserAchievementResponse(
            id=r.id,
            code=r.code,
            created_at=r.created_at.isoformat(),
        )
        for r in rows
    ]


@app.post("/sync/batch", response_model=SyncBatchResponse)
def sync_batch(
    req: SyncBatchRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db),
) -> SyncBatchResponse:
    processed = 0
    failed = 0
    errors = []
    
    for item in req.items:
        try:
            if item.entity_type == "water" and item.action == "create":
                payload = item.payload
                row = WaterEntry(
                    user_id=user.id,
                    date_epoch_day=payload["date_epoch_day"],
                    amount_ml=payload["amount_ml"],
                    created_at=now(),
                )
                db.add(row)
            elif item.entity_type == "food" and item.action == "create":
                payload = item.payload
                row = FoodEntry(
                    user_id=user.id,
                    date_epoch_day=payload["date_epoch_day"],
                    title=payload["title"],
                    calories=payload["calories"],
                    created_at=now(),
                )
                db.add(row)
            elif item.entity_type == "training" and item.action == "create":
                payload = item.payload
                row = TrainingEntry(
                    user_id=user.id,
                    date_epoch_day=payload["date_epoch_day"],
                    title=payload["title"],
                    description=payload.get("description"),
                    calories_burned=payload.get("calories_burned", 0),
                    duration_minutes=payload.get("duration_minutes", 0),
                    created_at=now(),
                )
                db.add(row)
            elif item.entity_type == "book" and item.action == "create":
                payload = item.payload
                row = BookEntry(
                    user_id=user.id,
                    title=payload["title"],
                    author=payload.get("author"),
                    total_pages=payload["total_pages"],
                    pages_read=0,
                    created_at=now(),
                )
                db.add(row)
            elif item.entity_type == "book" and item.action == "update_progress":
                payload = item.payload
                row = db.get(BookEntry, payload["id"])
                if row and row.user_id == user.id:
                    row.pages_read = payload["pages_read"]
            elif item.entity_type == "xp_event" and item.action == "create":
                payload = item.payload
                row = XpEvent(
                    user_id=user.id,
                    date_epoch_day=payload["date_epoch_day"],
                    type=payload["type"],
                    points=payload["points"],
                    note=payload.get("note"),
                    created_at=now(),
                )
                db.add(row)
            elif item.entity_type == "weight" and item.action == "upsert":
                payload = item.payload
                row = (
                    db.execute(
                        select(WeightEntry).where(
                            WeightEntry.user_id == user.id,
                            WeightEntry.date_epoch_day == payload["date_epoch_day"],
                        )
                    )
                    .scalars()
                    .one_or_none()
                )
                if row is None:
                    row = WeightEntry(
                        user_id=user.id,
                        date_epoch_day=payload["date_epoch_day"],
                        weight_kg=payload["weight_kg"],
                        created_at=now(),
                        updated_at=now(),
                    )
                    db.add(row)
                else:
                    row.weight_kg = payload["weight_kg"]
                    row.updated_at = now()
            elif item.entity_type == "smoke_status" and item.action == "upsert":
                payload = item.payload
                started_at = dt.datetime.fromisoformat(payload["started_at"].replace("Z", "+00:00"))
                row = db.execute(select(SmokeStatus).where(SmokeStatus.user_id == user.id)).scalar_one_or_none()
                if row is None:
                    row = SmokeStatus(
                        user_id=user.id,
                        started_at=started_at,
                        is_active=payload["is_active"],
                        pack_price=payload.get("pack_price", 0.0),
                        packs_per_day=payload.get("packs_per_day", 0.0),
                        updated_at=now(),
                    )
                    db.add(row)
                else:
                    row.is_active = payload["is_active"]
                    row.pack_price = payload.get("pack_price", row.pack_price)
                    row.packs_per_day = payload.get("packs_per_day", row.packs_per_day)
                    row.updated_at = now()
            elif item.entity_type == "steps" and item.action == "upsert":
                payload = item.payload
                row = (
                    db.execute(
                        select(StepEntry).where(
                            StepEntry.user_id == user.id,
                            StepEntry.date_epoch_day == payload["date_epoch_day"],
                        )
                    )
                    .scalars()
                    .one_or_none()
                )
                if row is None:
                    row = StepEntry(
                        user_id=user.id,
                        date_epoch_day=payload["date_epoch_day"],
                        steps=payload["steps"],
                        updated_at=now(),
                    )
                    db.add(row)
                else:
                    row.steps = payload["steps"]
                    row.updated_at = now()
            else:
                errors.append(f"Unsupported entity/action: {item.entity_type}/{item.action}")
                failed += 1
                continue
            
            processed += 1
        except Exception as e:
            errors.append(f"Error processing {item.entity_type}/{item.action}: {str(e)}")
            failed += 1
    
    db.commit()
    return SyncBatchResponse(processed=processed, failed=failed, errors=errors)


def require_admin_user(request: Request, db: Session = Depends(get_db)) -> AdminUser:
    return require_admin_session(request, db)


@app.get("/admin/api/settings", response_model=AdminSettingsResponse)
def get_admin_settings(
    db: Session = Depends(get_db),
) -> AdminSettingsResponse:
    row = db.execute(select(AdminSettings)).scalar_one_or_none()
    if row is None:
        # Create default settings
        row = AdminSettings(
            openrouter_api_key=None,
            openrouter_model=None,
            appodeal_app_key=None,
            appodeal_enabled=False,
            appodeal_banner_enabled=True,
            appodeal_interstitial_enabled=True,
            appodeal_rewarded_enabled=True,
            privacy_policy_text=None,
            privacy_policy_updated_at=None,
            announcement_enabled=False,
            announcement_text=None,
            announcement_button_enabled=False,
            announcement_button_text=None,
            announcement_button_url=None,
            announcement_updated_at=None,
            updated_at=now(),
        )
        db.add(row)
        db.commit()
        db.refresh(row)
    
    return AdminSettingsResponse(
        openrouter_api_key=row.openrouter_api_key,
        openrouter_model=row.openrouter_model,
        appodeal_app_key=row.appodeal_app_key,
        appodeal_enabled=row.appodeal_enabled,
        appodeal_banner_enabled=row.appodeal_banner_enabled,
        appodeal_interstitial_enabled=row.appodeal_interstitial_enabled,
        appodeal_rewarded_enabled=row.appodeal_rewarded_enabled,
        privacy_policy_text=row.privacy_policy_text,
        privacy_policy_updated_at=row.privacy_policy_updated_at.isoformat() if row.privacy_policy_updated_at else None,
        announcement_enabled=row.announcement_enabled,
        announcement_text=row.announcement_text,
        announcement_button_enabled=row.announcement_button_enabled,
        announcement_button_text=row.announcement_button_text,
        announcement_button_url=row.announcement_button_url,
        announcement_updated_at=row.announcement_updated_at.isoformat() if row.announcement_updated_at else None,
        updated_at=row.updated_at.isoformat(),
    )


@app.put("/admin/api/settings", response_model=AdminSettingsResponse)
def update_admin_settings(
    req: AdminSettingsRequest,
    admin_user: AdminUser = Depends(require_admin_user),
    db: Session = Depends(get_db),
) -> AdminSettingsResponse:
    row = db.execute(select(AdminSettings)).scalar_one_or_none()
    if row is None:
        row = AdminSettings(
            openrouter_api_key=req.openrouter_api_key,
            openrouter_model=req.openrouter_model,
            appodeal_app_key=req.appodeal_app_key,
            appodeal_enabled=req.appodeal_enabled or False,
            appodeal_banner_enabled=req.appodeal_banner_enabled if req.appodeal_banner_enabled is not None else True,
            appodeal_interstitial_enabled=req.appodeal_interstitial_enabled if req.appodeal_interstitial_enabled is not None else True,
            appodeal_rewarded_enabled=req.appodeal_rewarded_enabled if req.appodeal_rewarded_enabled is not None else True,
            privacy_policy_text=req.privacy_policy_text,
            privacy_policy_updated_at=now() if req.privacy_policy_text else None,
            announcement_enabled=req.announcement_enabled or False,
            announcement_text=req.announcement_text,
            announcement_button_enabled=req.announcement_button_enabled or False,
            announcement_button_text=req.announcement_button_text,
            announcement_button_url=req.announcement_button_url,
            announcement_updated_at=now() if req.announcement_text else None,
            updated_at=now(),
        )
        db.add(row)
    else:
        row.openrouter_api_key = req.openrouter_api_key
        row.openrouter_model = req.openrouter_model
        row.appodeal_app_key = req.appodeal_app_key
        if req.appodeal_enabled is not None:
            row.appodeal_enabled = req.appodeal_enabled
        if req.appodeal_banner_enabled is not None:
            row.appodeal_banner_enabled = req.appodeal_banner_enabled
        if req.appodeal_interstitial_enabled is not None:
            row.appodeal_interstitial_enabled = req.appodeal_interstitial_enabled
        if req.appodeal_rewarded_enabled is not None:
            row.appodeal_rewarded_enabled = req.appodeal_rewarded_enabled

        if req.privacy_policy_text is not None and req.privacy_policy_text != row.privacy_policy_text:
            row.privacy_policy_text = req.privacy_policy_text
            row.privacy_policy_updated_at = now()

        if req.announcement_enabled is not None:
            row.announcement_enabled = req.announcement_enabled
        if req.announcement_button_enabled is not None:
            row.announcement_button_enabled = req.announcement_button_enabled

        if req.announcement_text is not None and req.announcement_text != row.announcement_text:
            row.announcement_text = req.announcement_text
            row.announcement_updated_at = now()

        if req.announcement_button_text is not None:
            row.announcement_button_text = req.announcement_button_text
        if req.announcement_button_url is not None:
            row.announcement_button_url = req.announcement_button_url

        if row.announcement_button_enabled and row.announcement_button_text and row.announcement_button_url:
            pass
        elif row.announcement_button_enabled:
            row.announcement_button_enabled = False
        row.updated_at = now()
    
    db.commit()
    db.refresh(row)
    return AdminSettingsResponse(
        openrouter_api_key=row.openrouter_api_key,
        openrouter_model=row.openrouter_model,
        appodeal_app_key=row.appodeal_app_key,
        appodeal_enabled=row.appodeal_enabled,
        appodeal_banner_enabled=row.appodeal_banner_enabled,
        appodeal_interstitial_enabled=row.appodeal_interstitial_enabled,
        appodeal_rewarded_enabled=row.appodeal_rewarded_enabled,
        privacy_policy_text=row.privacy_policy_text,
        privacy_policy_updated_at=row.privacy_policy_updated_at.isoformat() if row.privacy_policy_updated_at else None,
        announcement_enabled=row.announcement_enabled,
        announcement_text=row.announcement_text,
        announcement_button_enabled=row.announcement_button_enabled,
        announcement_button_text=row.announcement_button_text,
        announcement_button_url=row.announcement_button_url,
        announcement_updated_at=row.announcement_updated_at.isoformat() if row.announcement_updated_at else None,
        updated_at=row.updated_at.isoformat(),
    )

