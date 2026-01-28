"""food_entries

Revision ID: 0007_food_entries
Revises: 0006_weight_and_smoke
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0007_food_entries"
down_revision = "0006_weight_and_smoke"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "food_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("title", sa.String(length=256), nullable=False),
        sa.Column("calories", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_food_entries_user_id", "food_entries", ["user_id"])
    op.create_index("ix_food_entries_date_epoch_day", "food_entries", ["date_epoch_day"])


def downgrade() -> None:
    op.drop_index("ix_food_entries_date_epoch_day", table_name="food_entries")
    op.drop_index("ix_food_entries_user_id", table_name="food_entries")
    op.drop_table("food_entries")
