-- noinspection SqlNoDataSourceInspectionForFile

-- Dataset Access
CREATE USER dataset_access WITH PASSWORD 'dataset_access';
CREATE DATABASE dataset_access;
GRANT ALL PRIVILEGES ON DATABASE dataset_access TO dataset_access;
