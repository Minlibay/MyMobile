"""announcements_table

Revision ID: 0017_announcements_table
Revises: 0016_announcement
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0017_announcements_table"
down_revision = "0016_announcement"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "announcements",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("title", sa.String(256), nullable=False),
        sa.Column("text", sa.Text(), nullable=False),
        sa.Column("button_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")),
        sa.Column("button_text", sa.String(64), nullable=True),
        sa.Column("button_url", sa.String(512), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False, server_default=sa.text("now()")),
    )
    op.create_index("idx_announcements_is_active", "announcements", ["is_active"])


def downgrade() -> None:
    op.drop_index("idx_announcements_is_active", table_name="announcements")
    op.drop_table("announcements")
