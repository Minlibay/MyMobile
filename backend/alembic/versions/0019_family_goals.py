"""family goals table

Revision ID: 0019_family_goals
Revises: 0018_family_invites
Create Date: 2026-02-02 20:10:00
"""

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '0019_family_goals'
down_revision = '0018_family_invites'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'family_goals',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('family_id', sa.Integer(), sa.ForeignKey('families.id'), nullable=False, index=True),
        sa.Column('week_start_epoch_day', sa.Integer(), nullable=False, index=True),
        sa.Column('steps_goal', sa.Integer(), nullable=False, server_default='70000'),
        sa.Column('trainings_goal', sa.Integer(), nullable=False, server_default='6'),
        sa.Column('water_goal_ml', sa.Integer(), nullable=False, server_default='21000'),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.func.now()),
        sa.UniqueConstraint('family_id', 'week_start_epoch_day', name='uq_family_goals_family_week'),
    )


def downgrade() -> None:
    op.drop_table('family_goals')
