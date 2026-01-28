"""xp_events_and_user_achievements

Revision ID: 0010_xp_events_and_user_achievements
Revises: 0009_book_entries
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0010_xp_events_and_user_achievements"
down_revision = "0009_book_entries"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "xp_events",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("type", sa.String(length=64), nullable=False),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("note", sa.String(length=256), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_xp_events_user_id", "xp_events", ["user_id"])
    op.create_index("ix_xp_events_date_epoch_day", "xp_events", ["date_epoch_day"])
    op.create_index("ix_xp_events_type", "xp_events", ["type"])
    op.create_index("ix_xp_events_created_at", "xp_events", ["created_at"])
    op.create_unique_constraint("uq_xp_events_user_day_type_note", "xp_events", ["user_id", "date_epoch_day", "type", "note"])

    op.create_table(
        "user_achievements",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("code", sa.String(length=128), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_user_achievements_user_id", "user_achievements", ["user_id"])
    op.create_index("ix_user_achievements_code", "user_achievements", ["code"])
    op.create_index("ix_user_achievements_created_at", "user_achievements", ["created_at"])
    op.create_unique_constraint("uq_user_achievements_user_code", "user_achievements", ["user_id", "code"])


def downgrade() -> None:
    op.drop_constraint("uq_user_achievements_user_code", "user_achievements", type_="unique")
    op.drop_index("ix_user_achievements_created_at", table_name="user_achievements")
    op.drop_index("ix_user_achievements_code", table_name="user_achievements")
    op.drop_index("ix_user_achievements_user_id", table_name="user_achievements")
    op.drop_table("user_achievements")
    op.drop_constraint("uq_xp_events_user_day_type_note", "xp_events", type_="unique")
    op.drop_index("ix_xp_events_created_at", table_name="xp_events")
    op.drop_index("ix_xp_events_type", table_name="xp_events")
    op.drop_index("ix_xp_events_date_epoch_day", table_name="xp_events")
    op.drop_index("ix_xp_events_user_id", table_name="xp_events")
    op.drop_table("xp_events")
