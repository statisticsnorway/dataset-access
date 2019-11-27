-- noinspection SqlNoDataSourceInspectionForFile

-- Create user and database
CREATE USER rdc WITH PASSWORD 'rdc';
CREATE DATABASE rdc;
GRANT ALL PRIVILEGES ON DATABASE rdc TO rdc;


-- Connect to database 'rdc' with user 'rdc'
\connect rdc rdc

-- Create dataset table
CREATE TABLE dataset
(
    datasetId varchar(100) PRIMARY KEY,
    document  jsonb
);

-- Create role table
CREATE TABLE role
(
    roleId   varchar(100) PRIMARY KEY,
    document jsonb
);

-- Create user table
CREATE TABLE user_permission
(
    userId   varchar(100) PRIMARY KEY,
    document jsonb
);
