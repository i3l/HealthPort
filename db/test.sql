CREATE SCHEMA cyborg;
 
CREATE TABLE cyborg.footable(
    id INTEGER NOT NULL,
    title VARCHAR(20) NOT NULL,
    PRIMARY KEY (id)
);
 
INSERT INTO cyborg.footable values (1,'Hello');
INSERT INTO cyborg.footable values (2,'World!');