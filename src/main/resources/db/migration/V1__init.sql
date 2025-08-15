CREATE TABLE users
(
    username    VARCHAR(50) PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    number      VARCHAR(20)  NOT NULL,
    city        VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20)  NOT NULL,
    country     VARCHAR(100) NOT NULL
);
