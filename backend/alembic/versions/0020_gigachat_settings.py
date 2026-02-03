"""add gigachat settings

Revision ID: 0020_gigachat_settings
Revises: 0019_family_goals
Create Date: 2026-02-03
"""

from alembic import op
import sqlalchemy as sa


revision = "0020_gigachat_settings"
down_revision = "0019_family_goals"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("admin_settings", sa.Column("gigachat_client_id", sa.String(length=128), nullable=True))
    op.add_column("admin_settings", sa.Column("gigachat_auth_key", sa.String(length=512), nullable=True))
    op.add_column("admin_settings", sa.Column("gigachat_scope", sa.String(length=64), nullable=True))


def downgrade() -> None:
    op.drop_column("admin_settings", "gigachat_scope")
    op.drop_column("admin_settings", "gigachat_auth_key")
    op.drop_column("admin_settings", "gigachat_client_id")
