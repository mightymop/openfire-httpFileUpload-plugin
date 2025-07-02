BEGIN
    EXECUTE IMMEDIATE '
    CREATE TABLE ofHttpfileupload (
        uid VARCHAR2(130) PRIMARY KEY,
        fromjid VARCHAR2(130) NOT NULL,
        inhalt BLOB,
        filename VARCHAR2(130) NOT NULL,
        time DATE NOT NULL,
        contenttype VARCHAR2(75),
        length NUMBER(19) NOT NULL
    )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN -- ORA-00955: name is already used by an existing object
            RAISE;
        END IF;
END;
/

MERGE INTO ofVersion v
USING (SELECT 'httpfileupload' AS name FROM dual) s
ON (v.name = s.name)
WHEN MATCHED THEN
    UPDATE SET v.version = 1
WHEN NOT MATCHED THEN
    INSERT (name, version) VALUES ('httpfileupload', 1);