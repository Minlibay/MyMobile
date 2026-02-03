"""family_invites

Revision ID: 0018_family_invites
Revises: 0017_announcements_table
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0018_family_invites"
down_revision = "0017_announcements_table"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        "family_invites",
        sa.Column("id", sa.Integer(), primary_key=True),
        sa.Column("family_id", sa.Integer(), sa.ForeignKey("families.id"), nullable=False),
        sa.Column("invited_user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("invited_by_user_id", sa.Integer(), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.UniqueConstraint("family_id", "invited_user_id", name="uq_family_invites_family_id_invited_user_id"),
    )
    op.create_index("ix_family_invites_invited_user_id", "family_invites", ["invited_user_id"])
    op.create_index("ix_family_invites_family_id", "family_invites", ["family_id"])


def downgrade() -> None:
    op.drop_index("ix_family_invites_family_id", table_name="family_invites")
    op.drop_index("ix_family_invites_invited_user_id", table_name="family_invites")
    op.drop_table("family_invites")
