"""water_entries

Revision ID: 0003_water_entries
Revises: 0002_step_entries
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0003_water_entries"
down_revision = "0002_step_entries"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "water_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("amount_ml", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_water_entries_user_id", "water_entries", ["user_id"])
    op.create_index("ix_water_entries_date_epoch_day", "water_entries", ["date_epoch_day"])
    op.create_index("ix_water_entries_created_at", "water_entries", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_water_entries_created_at", table_name="water_entries")
    op.drop_index("ix_water_entries_date_epoch_day", table_name="water_entries")
    op.drop_index("ix_water_entries_user_id", table_name="water_entries")
    op.drop_table("water_entries")
