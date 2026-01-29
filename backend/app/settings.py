from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=None, extra="ignore")

    ENV: str = "dev"
    DATABASE_URL: str

    JWT_SECRET: str
    ACCESS_TTL_SECONDS: int = 900
    REFRESH_TTL_SECONDS: int = 60 * 60 * 24 * 30

    ADMIN_API_KEY: str


settings = Settings()













