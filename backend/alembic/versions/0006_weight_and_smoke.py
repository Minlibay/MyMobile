"""weight_entries_and_smoke_status

Revision ID: 0006_weight_and_smoke
Revises: 0005_families
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0006_weight_and_smoke"
down_revision = "0005_families"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "weight_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("date_epoch_day", sa.Integer(), nullable=False),
        sa.Column("weight_kg", sa.Float(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("user_id", "date_epoch_day", name="uq_weight_entries_user_day"),
    )
    op.create_index("ix_weight_entries_user_id", "weight_entries", ["user_id"])
    op.create_index("ix_weight_entries_date_epoch_day", "weight_entries", ["date_epoch_day"])

    op.create_table(
        "smoke_status",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, server_default=sa.text("true")),
        sa.Column("pack_price", sa.Float(), nullable=False),
        sa.Column("packs_per_day", sa.Float(), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("user_id", name="uq_smoke_status_user_id"),
    )
    op.create_index("ix_smoke_status_user_id", "smoke_status", ["user_id"])


def downgrade() -> None:
    op.drop_index("ix_smoke_status_user_id", table_name="smoke_status")
    op.drop_table("smoke_status")

    op.drop_index("ix_weight_entries_date_epoch_day", table_name="weight_entries")
    op.drop_index("ix_weight_entries_user_id", table_name="weight_entries")
    op.drop_table("weight_entries")
