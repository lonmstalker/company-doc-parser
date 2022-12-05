CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS companies
(
    company_name      VARCHAR(200) PRIMARY KEY,
    "data"            JSONB NOT NULL,
    inner_companies   JSONB NULL,
    "references"      JSONB NULL
);