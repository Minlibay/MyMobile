"""families

Revision ID: 0005_families
Revises: 0004_admin_users
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0005_families"
down_revision = "0004_admin_users"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "families",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("name", sa.String(length=64), nullable=False),
        sa.Column("admin_user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("name", name="uq_families_name"),
    )
    op.create_index("ix_families_name", "families", ["name"])
    op.create_index("ix_families_admin_user_id", "families", ["admin_user_id"])

    op.create_table(
        "family_members",
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id"), primary_key=True),
        sa.Column("user_id", sa.Integer(), sa.ForeignKey("users.id"), primary_key=True),
        sa.Column("joined_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("family_id", "user_id", name="uq_family_members_family_id_user_id"),
    )
    op.create_index("ix_family_members_user_id", "family_members", ["user_id"])


def downgrade() -> None:
    op.drop_index("ix_family_members_user_id", table_name="family_members")
    op.drop_table("family_members")

    op.drop_index("ix_families_admin_user_id", table_name="families")
    op.drop_index("ix_families_name", table_name="families")
    op.drop_table("families")
