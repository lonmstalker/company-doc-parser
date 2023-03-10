CREATE ROLE parser WITH PASSWORD 'parser';
REVOKE CONNECT ON DATABASE parser FROM public;
ALTER ROLE parser WITH LOGIN;
GRANT CONNECT ON DATABASE parser TO parser;
GRANT USAGE ON SCHEMA public TO parser;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO parser WITH GRANT OPTION;
ALTER DEFAULT PRIVILEGES FOR ROLE parser IN SCHEMA public  GRANT SELECT ON TABLES TO parser;