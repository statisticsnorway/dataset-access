-- noinspection SqlNoDataSourceInspectionForFile

CREATE TABLE role
(
    roleId   varchar(100) PRIMARY KEY,
    document jsonb
);

CREATE TABLE user_permission
(
    userId   varchar(100) PRIMARY KEY,
    document jsonb
);
