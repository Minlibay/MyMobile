"""book_entries

Revision ID: 0009_book_entries
Revises: 0008_training_entries
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0009_book_entries"
down_revision = "0008_training_entries"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "book_entries",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("title", sa.String(length=256), nullable=False),
        sa.Column("author", sa.String(length=256), nullable=True),
        sa.Column("total_pages", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("pages_read", sa.Integer(), nullable=False, server_default=sa.text("0")),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index("ix_book_entries_user_id", "book_entries", ["user_id"])
    op.create_index("ix_book_entries_created_at", "book_entries", ["created_at"])


def downgrade() -> None:
    op.drop_index("ix_book_entries_created_at", table_name="book_entries")
    op.drop_index("ix_book_entries_user_id", table_name="book_entries")
    op.drop_table("book_entries")
