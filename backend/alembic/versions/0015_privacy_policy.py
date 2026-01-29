"""privacy_policy

Revision ID: 0015_privacy_policy
Revises: 0014_target_weight
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0015_privacy_policy"
down_revision = "0014_target_weight"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("admin_settings", sa.Column("privacy_policy_text", sa.Text(), nullable=True))
    op.add_column("admin_settings", sa.Column("privacy_policy_updated_at", sa.DateTime(timezone=True), nullable=True))

    op.add_column("user_settings", sa.Column("privacy_policy_accepted_at", sa.DateTime(timezone=True), nullable=True))
    op.add_column(
        "user_settings",
        sa.Column("privacy_policy_accepted_policy_updated_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("user_settings", "privacy_policy_accepted_policy_updated_at")
    op.drop_column("user_settings", "privacy_policy_accepted_at")

    op.drop_column("admin_settings", "privacy_policy_updated_at")
    op.drop_column("admin_settings", "privacy_policy_text")
