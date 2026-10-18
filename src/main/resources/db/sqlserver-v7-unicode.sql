IF COL_LENGTH('cinemas', 'name') IS NOT NULL
    ALTER TABLE cinemas ALTER COLUMN name NVARCHAR(120) NOT NULL;
IF COL_LENGTH('cinemas', 'city') IS NOT NULL
    ALTER TABLE cinemas ALTER COLUMN city NVARCHAR(100) NOT NULL;
IF COL_LENGTH('cinemas', 'address') IS NOT NULL
    ALTER TABLE cinemas ALTER COLUMN address NVARCHAR(255) NOT NULL;
IF COL_LENGTH('cinemas', 'phone') IS NOT NULL
    ALTER TABLE cinemas ALTER COLUMN phone NVARCHAR(30) NULL;
UPDATE cinemas SET city = N'TP. Hồ Chí Minh' WHERE city LIKE N'TP. H% Chí Minh';
