"""announcement

Revision ID: 0016_announcement
Revises: 0015_privacy_policy
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0016_announcement"
down_revision = "0015_privacy_policy"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("admin_settings", sa.Column("announcement_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")))
    op.add_column("admin_settings", sa.Column("announcement_text", sa.Text(), nullable=True))
    op.add_column("admin_settings", sa.Column("announcement_button_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")))
    op.add_column("admin_settings", sa.Column("announcement_button_text", sa.String(length=64), nullable=True))
    op.add_column("admin_settings", sa.Column("announcement_button_url", sa.String(length=512), nullable=True))
    op.add_column("admin_settings", sa.Column("announcement_updated_at", sa.DateTime(timezone=True), nullable=True))

    op.add_column("user_settings", sa.Column("announcement_read_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column("user_settings", sa.Column("announcement_read_announcement_updated_at", sa.DateTime(timezone=True), nullable=True))


def downgrade() -> None:
    op.drop_column("user_settings", "announcement_read_announcement_updated_at")
    op.drop_column("user_settings", "announcement_read_at")

    op.drop_column("admin_settings", "announcement_updated_at")
    op.drop_column("admin_settings", "announcement_button_url")
    op.drop_column("admin_settings", "announcement_button_text")
    op.drop_column("admin_settings", "announcement_button_enabled")
    op.drop_column("admin_settings", "announcement_text")
    op.drop_column("admin_settings", "announcement_enabled")
