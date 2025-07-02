DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables 
                   WHERE table_name = 'ofhttpfileupload') THEN
        CREATE TABLE ofHttpfileupload (
            uid VARCHAR(130) PRIMARY KEY,
            fromjid VARCHAR(130) NOT NULL,
            inhalt BYTEA,
            filename VARCHAR(130) NOT NULL,
            time TIMESTAMP NOT NULL,
            contenttype VARCHAR(75),
            length BIGINT NOT NULL
        );
    END IF;

    UPDATE ofVersion SET version = 1 WHERE name = 'httpfileupload';

    IF NOT FOUND THEN
        INSERT INTO ofVersion (name, version) VALUES ('httpfileupload', 1);
    END IF;
END$$;