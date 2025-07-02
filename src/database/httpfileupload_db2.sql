BEGIN
    DECLARE CONTINUE HANDLER FOR SQLSTATE '42710' BEGIN END; -- table already exists

    EXECUTE IMMEDIATE '
    CREATE TABLE ofHttpfileupload (
        uid VARCHAR(130) NOT NULL PRIMARY KEY,
        fromjid VARCHAR(130) NOT NULL,
        inhalt BLOB,
        filename VARCHAR(130) NOT NULL,
        time TIMESTAMP NOT NULL,
        contenttype VARCHAR(75),
        length BIGINT NOT NULL
    )';
END;
/

MERGE INTO ofVersion AS v
USING (SELECT 'httpfileupload' AS name FROM SYSIBM.SYSDUMMY1) AS src
ON v.name = src.name
WHEN MATCHED THEN
    UPDATE SET version = 1
WHEN NOT MATCHED THEN
    INSERT (name, version) VALUES ('httpfileupload', 1);