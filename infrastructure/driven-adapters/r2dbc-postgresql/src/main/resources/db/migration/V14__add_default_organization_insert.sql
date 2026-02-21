INSERT INTO organization_configuration (organization_id, max_units_per_distribution)
SELECT id, 100 FROM organization WHERE deleted_at IS NULL
ON DUPLICATE KEY UPDATE updated_at = NOW();
