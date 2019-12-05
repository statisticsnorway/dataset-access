-- noinspection SqlNoDataSourceInspectionForFile

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
