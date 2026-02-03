# Обновление бекенда: Синхронизация профиля пользователя

## Проблема
Профиль пользователя (рост, вес, возраст, пол) хранится только локально в Android приложении. При переустановке приложения профиль теряется, и пользователю приходится заполнять его заново.

## Решение
Добавить API эндпоинты для сохранения и получения профиля пользователя на бекенде.

## Что нужно добавить в бекенд

### 1. Обновить модель базы данных (`app/models.py`)

Добавить модель `Profile`:

```python
class Profile(Base):
    __tablename__ = "profiles"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[int] = mapped_column(ForeignKey("users.id"), unique=True, index=True)
    height_cm: Mapped[int] = mapped_column(Integer)
    weight_kg: Mapped[float] = mapped_column(Numeric(5, 2))
    age: Mapped[int] = mapped_column(Integer)
    sex: Mapped[str] = mapped_column(String(10))  # "male" | "female"
    created_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
    updated_at: Mapped[dt.datetime] = mapped_column(DateTime(timezone=True), default=lambda: dt.datetime.now(dt.timezone.utc))
```

### 2. Добавить схемы (`app/schemas.py`)

```python
class ProfileRequest(BaseModel):
    height_cm: int = Field(ge=80, le=250)
    weight_kg: float = Field(ge=20.0, le=400.0)
    age: int = Field(ge=5, le=120)
    sex: str = Field(pattern="^(male|female)$")

class ProfileResponse(BaseModel):
    height_cm: int
    weight_kg: float
    age: int
    sex: str
    created_at: str
    updated_at: str
```

### 3. Добавить эндпоинты (`app/main.py`)

```python
@app.get("/users/me/profile", response_model=ProfileResponse)
def get_profile(user: User = Depends(require_user), db: Session = Depends(get_db)) -> ProfileResponse:
    profile = db.execute(select(Profile).where(Profile.user_id == user.id)).scalar_one_or_none()
    if profile is None:
        raise HTTPException(status_code=404, detail="Profile not found")
    return ProfileResponse(
        height_cm=profile.height_cm,
        weight_kg=float(profile.weight_kg),
        age=profile.age,
        sex=profile.sex,
        created_at=profile.created_at.isoformat(),
        updated_at=profile.updated_at.isoformat()
    )

@app.put("/users/me/profile", response_model=ProfileResponse)
def update_profile(
    req: ProfileRequest,
    user: User = Depends(require_user),
    db: Session = Depends(get_db)
) -> ProfileResponse:
    profile = db.execute(select(Profile).where(Profile.user_id == user.id)).scalar_one_or_none()
    now = dt.datetime.now(dt.timezone.utc)
    
    if profile is None:
        profile = Profile(
            user_id=user.id,
            height_cm=req.height_cm,
            weight_kg=req.weight_kg,
            age=req.age,
            sex=req.sex,
            created_at=now,
            updated_at=now
        )
        db.add(profile)
    else:
        profile.height_cm = req.height_cm
        profile.weight_kg = req.weight_kg
        profile.age = req.age
        profile.sex = req.sex
        profile.updated_at = now
    
    db.commit()
    db.refresh(profile)
    
    return ProfileResponse(
        height_cm=profile.height_cm,
        weight_kg=float(profile.weight_kg),
        age=profile.age,
        sex=profile.sex,
        created_at=profile.created_at.isoformat(),
        updated_at=profile.updated_at.isoformat()
    )
```

### 4. Создать миграцию Alembic

```bash
cd /opt/com.volovod.com.volovod.altavolovod.alta-backend/backend
docker-compose exec api alembic revision --autogenerate -m "add_profile_table"
docker-compose exec api alembic upgrade head
```

## Файлы для изменения

1. `app/models.py` - добавить модель `Profile`
2. `app/schemas.py` - добавить `ProfileRequest` и `ProfileResponse`
3. `app/main.py` - добавить эндпоинты `GET /users/me/profile` и `PUT /users/me/profile`
4. Создать миграцию Alembic для таблицы `profiles`

## После обновления

После добавления этих эндпоинтов Android приложение сможет:
- Сохранять профиль на бекенд при создании/обновлении
- Загружать профиль с бекенда при входе
- Синхронизировать профиль между устройствами










