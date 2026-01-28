"""admin_settings

Revision ID: 0012_admin
Revises: 0011_sync
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0012_admin"
down_revision = "0011_sync"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "admin_settings",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("openrouter_api_key", sa.String(length=256), nullable=True),
        sa.Column("openrouter_model", sa.String(length=128), nullable=True),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_admin_settings_updated_at", "admin_settings", ["updated_at"])


def downgrade() -> None:
    op.drop_index("ix_admin_settings_updated_at", table_name="admin_settings")
    op.drop_table("admin_settings")
