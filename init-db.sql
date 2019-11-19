-- Create user and database
CREATE USER rdc WITH PASSWORD 'rdc';
CREATE DATABASE rdc;
GRANT ALL PRIVILEGES ON DATABASE rdc TO rdc;


-- Connect to database 'rdc' with user 'rdc'
\connect rdc rdc


-- Create dataset table
CREATE TABLE public.dataset
(
    id varchar(100) NOT NULL CONSTRAINT dataset_pk PRIMARY KEY
);

ALTER TABLE dataset OWNER TO rdc;

CREATE UNIQUE INDEX dataset_id_uindex ON dataset (id);


-- Create user table
CREATE TABLE dataset_user
(
    id varchar(100) NOT NULL CONSTRAINT user_pk PRIMARY KEY
);

ALTER TABLE dataset_user OWNER TO rdc;

CREATE UNIQUE INDEX user_id_uindex ON dataset_user (id);


-- Create permission table
CREATE TABLE dataset_user_permission
(
    dataset_id varchar(100) NOT NULL REFERENCES dataset(id),
    dataset_user_id varchar(100) NOT NULL REFERENCES dataset_user(id),
    CONSTRAINT dataset_user_permission_pkey PRIMARY KEY (dataset_id, dataset_user_id)
);

ALTER TABLE dataset_user_permission OWNER TO rdc;
