--liquibase formatted sql

--changeset hieu:002
ALTER TABLE users ADD COLUMN name VARCHAR(100);

--rollback ALTER TABLE users DROP COLUMN name;
