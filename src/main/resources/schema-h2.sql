-- Spring Batch schema for H2 Database
create table BATCH_JOB_INSTANCE (
  JOB_INSTANCE_ID bigint not null auto_increment primary key,
  JOB_NAME varchar(100) not null,
  JOB_KEY bigint not null,
  constraint JOB_NAME_UNIQUE unique (JOB_NAME, JOB_KEY)
);

create table BATCH_JOB_EXECUTION (
  JOB_EXECUTION_ID bigint not null auto_increment primary key,
  JOB_INSTANCE_ID bigint not null,
  CREATE_TIME timestamp not null,
  START_TIME timestamp,
  END_TIME timestamp,
  STATUS varchar(10) not null,
  EXIT_CODE varchar(255) not null,
  EXIT_MESSAGE varchar(255),
  LAST_UPDATED timestamp not null,
  constraint FK_JOB_INSTANCE foreign key (JOB_INSTANCE_ID)
    references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

-- Other tables for Spring Batch (e.g., BATCH_STEP_EXECUTION, BATCH_JOB_EXECUTION_PARAMS, etc.)
