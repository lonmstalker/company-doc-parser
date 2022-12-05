CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS companies
(
    company_name      VARCHAR(200) PRIMARY KEY,
    "data"            JSONB NOT NULL,
    "references"      JSONB NULL
);

CREATE TABLE IF NOT EXISTS archives (
    archive_name     VARCHAR(200) PRIMARY KEY,
    archive_path     VARCHAR(200),
    archive_date     DATE,
    is_parsed        BOOLEAN      DEFAULT false
)