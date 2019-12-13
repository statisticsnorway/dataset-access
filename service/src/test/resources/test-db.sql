-- Create dataset table
CREATE TABLE dataset
(
    id varchar(100) PRIMARY KEY
);

ALTER TABLE dataset OWNER TO rdc;

-- Create user table
CREATE TABLE dataset_user
(
    id varchar(100) PRIMARY KEY
);

ALTER TABLE dataset_user OWNER TO rdc;


-- Create permission table
CREATE TABLE dataset_user_permission
(
    dataset_id varchar(100) NOT NULL,
    dataset_user_id varchar(100) NOT NULL,
    PRIMARY KEY (dataset_id, dataset_user_id)
);

ALTER TABLE dataset_user_permission OWNER TO rdc;


-- Insert some test data
INSERT INTO dataset VALUES ('DATASET_1');

INSERT INTO dataset_user VALUES ('USER_1');

INSERT INTO dataset_user_permission (dataset_id, dataset_user_id) VALUES ('DATASET_1', 'USER_1') ON CONFLICT DO NOTHING;
