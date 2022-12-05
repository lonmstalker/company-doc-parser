CREATE ROLE parser WITH PASSWORD 'parser';
REVOKE CONNECT ON DATABASE parser FROM public;
ALTER ROLE parser WITH LOGIN;
GRANT CONNECT ON DATABASE parser TO parser;
GRANT pg_read_all_data TO parser;
GRANT pg_write_all_data TO parser;
-- https://www.postgresql.org/docs/current/predefined-roles.html