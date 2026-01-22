import base64
import datetime as dt
import hashlib
import os
from typing import Any

from jose import jwt
from passlib.context import CryptContext

from app.settings import settings


pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(password: str, password_hash: str) -> bool:
    return pwd_context.verify(password, password_hash)


def _now() -> dt.datetime:
    return dt.datetime.now(dt.timezone.utc)


def create_access_token(user_id: int) -> str:
    now = _now()
    payload: dict[str, Any] = {
        "sub": str(user_id),
        "iat": int(now.timestamp()),
        "exp": int((now + dt.timedelta(seconds=settings.ACCESS_TTL_SECONDS)).timestamp()),
        "type": "access",
    }
    return jwt.encode(payload, settings.JWT_SECRET, algorithm="HS256")


def decode_access_token(token: str) -> dict[str, Any]:
    payload = jwt.decode(token, settings.JWT_SECRET, algorithms=["HS256"])
    if payload.get("type") != "access":
        raise ValueError("Not an access token")
    return payload


def new_refresh_token() -> str:
    raw = os.urandom(32)
    return base64.urlsafe_b64encode(raw).decode("utf-8").rstrip("=")


def hash_refresh_token(token: str) -> str:
    # sha256 hex is fine because token is high-entropy random
    return hashlib.sha256(token.encode("utf-8")).hexdigest()




