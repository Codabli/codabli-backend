\set ON_ERROR_STOP on

SELECT :'schema' ~ '^[a-z_][a-z0-9_]*$'
       AND length(:'schema') <= 63
       AND :'schema' <> 'public'
       AND :'schema' !~ '^pg_' AS valid_schema
\gset

\if :valid_schema
SELECT format('CREATE SCHEMA IF NOT EXISTS %I AUTHORIZATION %I', :'schema', current_user)
\gexec
\else
DO $$
BEGIN
    RAISE EXCEPTION 'Invalid schema: use a dedicated lowercase PostgreSQL identifier (max 63 characters), not public or pg_*.';
END
$$;
\endif
