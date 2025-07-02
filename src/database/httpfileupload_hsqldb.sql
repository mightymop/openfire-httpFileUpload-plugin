CREATE TABLE IF NOT EXISTS ofHttpfileupload (
    uid VARCHAR(130) NOT NULL PRIMARY KEY,
    fromjid VARCHAR(130) NOT NULL,
    inhalt LONGVARBINARY,
    filename VARCHAR(130) NOT NULL,
    time TIMESTAMP NOT NULL,
    contenttype VARCHAR(75),
    length BIGINT NOT NULL
);

MERGE INTO ofVersion (name, version)
KEY (name)
VALUES ('httpfileupload', 1);