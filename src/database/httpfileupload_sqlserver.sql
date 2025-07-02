-- SQL Server: Plugin Initialisierung
EXEC sp_executesql N'
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_NAME = ''ofHttpfileupload''
)
BEGIN
    CREATE TABLE dbo.ofHttpfileupload (
        uid NVARCHAR(130) PRIMARY KEY NOT NULL,
        fromjid NVARCHAR(255) NOT NULL,
        filename NVARCHAR(255) NOT NULL,
        time DATETIME NULL,
        length BIGINT NOT NULL,
        contenttype NVARCHAR(100) NULL,
        inhalt VARBINARY(MAX) NULL
    )
END';

UPDATE ofVersion SET version = 1 WHERE name = 'httpfileupload';
INSERT INTO ofVersion (name, version)
SELECT 'httpfileupload', 1
WHERE NOT EXISTS (SELECT 1 FROM ofVersion WHERE name = 'httpfileupload');