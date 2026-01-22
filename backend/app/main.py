import datetime as dt

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.db import Base, engine, get_db
from app.models import AdUnit, RefreshSession, User
from app.schemas import (
    AdUnitUpsert,
    AdsConfigResponse,
    LoginRequest,
    LogoutRequest,
    RefreshRequest,
    RegisterRequest,
    TokenPair,
    UserMeResponse,
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
    existing = db.execute(select(User).where(User.login == req.login)).scalar_one_or_none()
    if existing is not None:
        raise HTTPException(status_code=409, detail="Login already exists")
    user = User(login=req.login, password_hash=hash_password(req.password))
    db.add(user)
    db.commit()
    db.refresh(user)
    return UserMeResponse(id=user.id, login=user.login)


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


