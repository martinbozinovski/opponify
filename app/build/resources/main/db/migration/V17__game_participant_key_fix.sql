ALTER TABLE game_participants DROP CONSTRAINT IF EXISTS game_participants_pkey;
CREATE UNIQUE INDEX IF NOT EXISTS uq_game_user_participant ON game_participants(game_id,participant_user_id) WHERE participant_user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_game_team_participant ON game_participants(game_id,participant_team_id) WHERE participant_team_id IS NOT NULL;
ALTER TABLE game_participants ALTER COLUMN participant_user_id DROP NOT NULL;
ALTER TABLE game_participants ALTER COLUMN participant_team_id DROP NOT NULL;
