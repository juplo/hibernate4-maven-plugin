
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
        add constraint FK_cjsh8995uabmdm9b30uvmyj6p 
        foreign key (REV) 
        references TTL_AUDIT_REVISION;

    create sequence TTL_AUDIT_REVISION_SEQ;

    create sequence TTL_EVENT_SEQ;
