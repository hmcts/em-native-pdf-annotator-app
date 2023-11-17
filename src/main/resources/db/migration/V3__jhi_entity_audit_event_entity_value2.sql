ALTER TABLE jhi_entity_audit_event ADD COLUMN entity_value_v2 TEXT,
ADD COLUMN entity_value_migrated boolean DEFAULT false NOT NULL;
CREATE INDEX IF NOT EXISTS jhi_entity_audit_event_entity_value_migrated_index ON jhi_entity_audit_event(entity_value_migrated);
