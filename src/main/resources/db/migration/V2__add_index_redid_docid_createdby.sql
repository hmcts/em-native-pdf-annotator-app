CREATE INDEX CONCURRENTLY IF NOT EXISTS redactionid_documentid_createdby ON redaction (redaction_id,document_id, created_by);
