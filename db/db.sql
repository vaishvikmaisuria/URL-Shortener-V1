DROP TABLE IF EXISTS URLShorteners;

CREATE TABLE URLShorteners (
    short string PRIMARY KEY NOT NULL,
    long string NOT NULL
);
