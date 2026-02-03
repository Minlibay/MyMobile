"""training_entries

Revision ID: 0008_training_entries
Revises: 0007_food_entries
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0008_training_entries"
down_revision = "0007_food_entries"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "training_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=256), nullable=False),
        sa.Column("description", sa.String(length=512), nullable=True),
        sa.Column("calories_burned", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("duration_minutes", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_training_entries_user_id", "training_entries", ["user_id"])
    op.create_index("ix_training_entries_date_epoch_day", "training_entries", ["date_epoch_day"])


def downgrade() -> None:
    op.drop_index("ix_training_entries_date_epoch_day", table_name="training_entries")
    op.drop_index("ix_training_entries_user_id", table_name="training_entries")
    op.drop_table("training_entries")
