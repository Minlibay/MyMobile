import datetime as dt
from sqlalchemy import String, Integer, BigInteger, Boolean, DateTime, ForeignKey, UniqueConstraint, Float, JSON, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    login: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(256))
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    sessions: Mapped[list["RefreshSession"]] = relationship(back_populates="user")


class AdminUser(Base):
    __tablename__ = "admin_users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    username: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(256))
    created_at: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: dt.datetime.now(dt.timezone.utc),
    )


class RefreshSession(Base):
    __tablename__ = "refresh_sessions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    refresh_hash: Mapped[str] = mapped_column(String(128), index=True)
    device_id: Mapped[str | None] = mapped_column(String(128), nullable=True)

    expires_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), index=True)
    revoked_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
    last_used_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    user: Mapped[User] = relationship(back_populates="sessions")


class AdUnit(Base):
    __tablename__ = "ad_units"
    __table_args__ = (
        UniqueConstraint("network", "placement", name="uq_ad_units_network_placement"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    network: Mapped[str] = mapped_column(String(32), index=True)  # e.g. "yandex"
    placement: Mapped[str] = mapped_column(String(64), index=True)  # e.g. "banner_home"
    ad_unit_id: Mapped[str] = mapped_column(String(128))
    enabled: Mapped[bool] = mapped_column(Boolean, default=True)

    android_min_version: Mapped[int | None] = mapped_column(Integer, nullable=True)
    android_max_version: Mapped[int | None] = mapped_column(Integer, nullable=True)

    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))


class Profile(Base):
    __tablename__ = "profiles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True, index=True)
    height_cm: Mapped[int] = mapped_column(Integer)
    weight_kg: Mapped[float] = mapped_column(BigInteger)
    age: Mapped[int] = mapped_column(Integer)
    sex: Mapped[str] = mapped_column(String(16)) # "male" | "female"

    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship(back_populates="profile")


User.profile = relationship("Profile", back_populates="user", uselist=False)


class UserSettings(Base):
    __tablename__ = "user_settings"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True, index=True)
    calorie_mode: Mapped[str] = mapped_column(String(16)) # "maintain" | "lose" | "gain"
    step_goal: Mapped[int] = mapped_column(Integer)
    calorie_goal_override: Mapped[int | None] = mapped_column(Integer, nullable=True)
    target_weight_kg: Mapped[float | None] = mapped_column(Float, nullable=True)
    privacy_policy_accepted_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    privacy_policy_accepted_policy_updated_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    announcement_read_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    announcement_read_announcement_updated_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    reminders_enabled: Mapped[bool] = mapped_column(Boolean, default=True)

    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship(back_populates="user_settings")


User.user_settings = relationship("UserSettings", back_populates="user", uselist=False)


class Family(Base):
    __tablename__ = "families"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    admin_user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    members: Mapped[list["FamilyMember"]] = relationship(back_populates="family", cascade="all, delete-orphan")
    admin: Mapped[User] = relationship("User", foreign_keys=[admin_user_id])


class FamilyMember(Base):
    __tablename__ = "family_members"
    __table_args__ = (
        UniqueConstraint("family_id", "user_id", name="uq_family_members_family_id_user_id"),
    )

    family_id: Mapped[int] = mapped_column(ForeignKey("families.id"), primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), primary_key=True)
    joined_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    family: Mapped[Family] = relationship(back_populates="members")
    user: Mapped[User] = relationship(back_populates="family_members")


User.family_members = relationship("FamilyMember", back_populates="user", cascade="all, delete-orphan")


class StepEntry(Base):
    __tablename__ = "step_entries"
    __table_args__ = (
        UniqueConstraint("user_id", "date_epoch_day", name="uq_step_entries_user_day"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    steps: Mapped[int] = mapped_column(Integer)
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship()


class BookEntry(Base):
    __tablename__ = "book_entries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    title: Mapped[str] = mapped_column(String(256))
    author: Mapped[str | None] = mapped_column(String(256), nullable=True)
    total_pages: Mapped[int] = mapped_column(Integer, default=0)
    pages_read: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc), index=True)

    user: Mapped["User"] = relationship()


class XpEvent(Base):
    __tablename__ = "xp_events"
    __table_args__ = (
        UniqueConstraint("user_id", "date_epoch_day", "type", "note", name="uq_xp_events_user_day_type_note"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    type: Mapped[str] = mapped_column(String(64), index=True)
    points: Mapped[int] = mapped_column(Integer)
    note: Mapped[str | None] = mapped_column(String(256), nullable=True)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc), index=True)

    user: Mapped["User"] = relationship()


class UserAchievement(Base):
    __tablename__ = "user_achievements"
    __table_args__ = (
        UniqueConstraint("user_id", "code", name="uq_user_achievements_user_code"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    code: Mapped[str] = mapped_column(String(128), index=True)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc), index=True)

    user: Mapped["User"] = relationship()


class SyncQueue(Base):
    __tablename__ = "sync_queue"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    entity_type: Mapped[str] = mapped_column(String(64), index=True)
    action: Mapped[str] = mapped_column(String(32), index=True)
    payload: Mapped[dict] = mapped_column(JSON)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc), index=True)
    attempts: Mapped[int] = mapped_column(Integer, default=0)
    next_attempt_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)

    user: Mapped["User"] = relationship()


class AdminSettings(Base):
    __tablename__ = "admin_settings"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    openrouter_api_key: Mapped[str | None] = mapped_column(String(256), nullable=True)
    openrouter_model: Mapped[str | None] = mapped_column(String(128), nullable=True)
    appodeal_app_key: Mapped[str | None] = mapped_column(String(256), nullable=True)
    appodeal_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    appodeal_banner_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    appodeal_interstitial_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    appodeal_rewarded_enabled: Mapped[bool] = mapped_column(Boolean, default=True)
    privacy_policy_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    privacy_policy_updated_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    announcement_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    announcement_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    announcement_button_enabled: Mapped[bool] = mapped_column(Boolean, default=False)
    announcement_button_text: Mapped[str | None] = mapped_column(String(64), nullable=True)
    announcement_button_url: Mapped[str | None] = mapped_column(String(512), nullable=True)
    announcement_updated_at: Mapped[dt.datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc), index=True)


class TrainingEntry(Base):
    __tablename__ = "training_entries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    title: Mapped[str] = mapped_column(String(256))
    description: Mapped[str | None] = mapped_column(String(512), nullable=True)
    calories_burned: Mapped[int] = mapped_column(Integer, default=0)
    duration_minutes: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship()


class FoodEntry(Base):
    __tablename__ = "food_entries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    title: Mapped[str] = mapped_column(String(256))
    calories: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship()


class WeightEntry(Base):
    __tablename__ = "weight_entries"
    __table_args__ = (
        UniqueConstraint("user_id", "date_epoch_day", name="uq_weight_entries_user_day"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    weight_kg: Mapped[float] = mapped_column(Float)
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship()


class SmokeStatus(Base):
    __tablename__ = "smoke_status"
    __table_args__ = (
        UniqueConstraint("user_id", name="uq_smoke_status_user_id"),
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    started_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True))
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
    pack_price: Mapped[float] = mapped_column(Float)
    packs_per_day: Mapped[float] = mapped_column(Float)
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))

    user: Mapped["User"] = relationship()


class WaterEntry(Base):
    __tablename__ = "water_entries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True)
    date_epoch_day: Mapped[int] = mapped_column(Integer, index=True)
    amount_ml: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[dt.datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: dt.datetime.now(dt.timezone.utc),
        index=True,
    )

    user: Mapped["User"] = relationship()








