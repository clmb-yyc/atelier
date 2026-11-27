-- Keep existing rows and foreign keys when upgrading an older Atelier database.
DO '
BEGIN
    ALTER TABLE IF EXISTS access_plans RENAME TO passes;
    ALTER TABLE IF EXISTS member_passes RENAME TO user_passes;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = ''passes'' AND column_name = ''plan_type''
    ) THEN
        ALTER TABLE passes RENAME COLUMN plan_type TO pass_type;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = ''user_passes'' AND column_name = ''access_plan_id''
    ) THEN
        ALTER TABLE user_passes RENAME COLUMN access_plan_id TO pass_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = ''check_ins'' AND column_name = ''member_pass_id''
    ) THEN
        ALTER TABLE check_ins RENAME COLUMN member_pass_id TO user_pass_id;
    END IF;

    ALTER INDEX IF EXISTS member_passes_account_id_idx RENAME TO user_passes_account_id_idx;
    ALTER INDEX IF EXISTS check_ins_member_pass_id_idx RENAME TO check_ins_user_pass_id_idx;
END;
';
