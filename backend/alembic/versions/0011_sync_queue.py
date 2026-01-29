"""sync_queue

Revision ID: 0011_sync
Revises: 0010_xp
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0011_sync"
down_revision = "0010_xp"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "sync_queue",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("entity_type", sa.String(length=64), nullable=False),
        sa.Column("action", sa.String(length=32), nullable=False),  # "create", "update", "delete"
        sa.Column("payload", sa.JSON(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("attempts", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("next_attempt_at", sa.DateTime(timezone=True), nullable=True),
    )
    op.create_index("ix_sync_queue_user_id", "sync_queue", ["user_id"])
    op.create_index("ix_sync_queue_entity_type", "sync_queue", ["entity_type"])
    op.create_index("ix_sync_queue_next_attempt_at", "sync_queue", ["next_attempt_at"])
    op.create_index("ix_sync_queue_created_at", "sync_queue", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_sync_queue_created_at", table_name="sync_queue")
    op.drop_index("ix_sync_queue_next_attempt_at", table_name="sync_queue")
    op.drop_index("ix_sync_queue_entity_type", table_name="sync_queue")
    op.drop_index("ix_sync_queue_user_id", table_name="sync_queue")
    op.drop_table("sync_queue")
