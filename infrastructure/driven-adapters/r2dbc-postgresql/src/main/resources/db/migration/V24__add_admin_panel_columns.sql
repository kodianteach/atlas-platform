-- V24: Add columns for admin panel support (HU #12)
-- MySQL syntax

ALTER TABLE comments ADD COLUMN flag_reason VARCHAR(500) NULL, ADD COLUMN author_role VARCHAR(50) NULL;
