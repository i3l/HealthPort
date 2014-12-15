CREATE SCHEMA cdcAppDB;
 
CREATE TABLE cdcAppDB.Users(
    name VARCHAR(100) NOT NULL,
    location VARCHAR(10) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE cdcAppDB.HVUsers(
    name VARCHAR(100) NOT NULL,
    recordID VARCHAR(50) NOT NULL,
    personID VARCHAR(50) NOT NULL,
    PRIMARY KEY (personID)
);
 
