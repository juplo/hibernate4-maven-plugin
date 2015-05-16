
    drop table EVENTS if exists;

    create table EVENTS (
        id bigint not null,
        EVENT_DATE timestamp,
        title varchar(255),
        primary key (id)
    );
