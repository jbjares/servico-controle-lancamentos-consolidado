CREATE SCHEMA IF NOT EXISTS lancamentos AUTHORIZATION app;
CREATE SCHEMA IF NOT EXISTS consolidado AUTHORIZATION app;

CREATE TABLE IF NOT EXISTS lancamentos.databasechangeloglock (
    id INTEGER NOT NULL PRIMARY KEY,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255)
);

INSERT INTO lancamentos.databasechangeloglock (id, locked, lockgranted, lockedby)
VALUES (1, FALSE, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS lancamentos.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS consolidado.databasechangeloglock (
    id INTEGER NOT NULL PRIMARY KEY,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255)
);

INSERT INTO consolidado.databasechangeloglock (id, locked, lockgranted, lockedby)
VALUES (1, FALSE, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS consolidado.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);
