CREATE TABLE deleted (
  id integer NOT NULL generated by default as identity (START WITH 1, INCREMENT BY 1),
  identifier varchar(255) default NULL,
  created_at timestamp default NULL,
  PRIMARY KEY  (id)
)

CREATE INDEX IDX_Deleted_Identifier ON deleted(identifier)