CREATE TABLE IF NOT EXISTS ofHttpfileupload (
    uid VARCHAR(130) NOT NULL,
    fromjid VARCHAR(130) NOT NULL,
    inhalt LONGBLOB,
    filename VARCHAR(130) NOT NULL,
    time DATETIME NOT NULL,
    contenttype VARCHAR(75),
    length BIGINT NOT NULL,
    PRIMARY KEY (uid)
);

INSERT INTO ofVersion (name, version)
VALUES ('httpfileupload', 1)
ON DUPLICATE KEY UPDATE version = 1;