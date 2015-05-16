
    drop table EVENTS if exists;

    create table EVENTS (
        EVENT_ID bigint not null,
        EVENT_DATE timestamp,
        title varchar(255),
        primary key (EVENT_ID)
    );
