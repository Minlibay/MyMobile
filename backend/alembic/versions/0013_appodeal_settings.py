"""appodeal_settings

Revision ID: 0013_appodeal
Revises: 0012_admin
Create Date: 2026-01-28

"""

from __future__ import annotations

import sqlalchemy as sa
from alembic import op

revision = "0013_appodeal"
down_revision = "0012_admin"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.add_column("admin_settings", sa.Column("appodeal_app_key", sa.String(length=256), nullable=True))
    op.add_column("admin_settings", sa.Column("appodeal_enabled", sa.Boolean(), nullable=False, server_default=sa.text("false")))
    op.add_column("admin_settings", sa.Column("appodeal_banner_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")))
    op.add_column("admin_settings", sa.Column("appodeal_interstitial_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")))
    op.add_column("admin_settings", sa.Column("appodeal_rewarded_enabled", sa.Boolean(), nullable=False, server_default=sa.text("true")))


def downgrade() -> None:
    op.drop_column("admin_settings", "appodeal_rewarded_enabled")
    op.drop_column("admin_settings", "appodeal_interstitial_enabled")
    op.drop_column("admin_settings", "appodeal_banner_enabled")
    op.drop_column("admin_settings", "appodeal_enabled")
    op.drop_column("admin_settings", "appodeal_app_key")
