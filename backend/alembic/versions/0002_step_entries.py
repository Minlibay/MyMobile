"""step_entries

Revision ID: 0002_step_entries
Revises: 0001_init
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0002_step_entries"
down_revision = "0001_init"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "step_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("steps", sa.Integer(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("user_id", "date_epoch_day", name="uq_step_entries_user_day"),
    )
    op.create_index("ix_step_entries_user_id", "step_entries", ["user_id"])
    op.create_index("ix_step_entries_date_epoch_day", "step_entries", ["date_epoch_day"])


def downgrade() -> None:
    op.drop_index("ix_step_entries_date_epoch_day", table_name="step_entries")
    op.drop_index("ix_step_entries_user_id", table_name="step_entries")
    op.drop_table("step_entries")


