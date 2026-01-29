"""target_weight_in_user_settings

Revision ID: 0014_target_weight
Revises: 0013_appodeal
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0014_target_weight"
down_revision = "0013_appodeal"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("user_settings", sa.Column("target_weight_kg", sa.Float(), nullable=True))


def downgrade() -> None:
    op.drop_column("user_settings", "target_weight_kg")
