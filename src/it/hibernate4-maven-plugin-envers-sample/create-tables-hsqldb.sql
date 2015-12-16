
    create sequence TTL_AUDIT_REVISION_SEQ start with 1 increment by 10;

    create sequence TTL_EVENT_SEQ start with 1 increment by 10;

    create table TTL_AUDIT_REVISION (
        ID bigint not null,
        EVENT_DATE timestamp,
        ENVERS_TSTAMP bigint not null,
        USER_NAME varchar(80) not null,
        primary key (ID)
    );

    create table TTL_EVENT (
        ID bigint not null,
        EVENT_DATE timestamp not null,
        TITLE varchar(80) not null,
        primary key (ID)
    );

    create table TTL_EVENT_AUD (
        ID bigint not null,
        REV bigint not null,
        REVTYPE tinyint,
        EVENT_DATE timestamp,
        TITLE varchar(80),
        primary key (ID, REV)
    );

    alter table TTL_EVENT_AUD 
        add constraint FK295td34se5kaxwgo8i1ph0k4 
        foreign key (REV) 
        references TTL_AUDIT_REVISION;
