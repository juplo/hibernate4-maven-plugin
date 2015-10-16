
    create sequence hibernate_sequence start 1 increment 1;

    create table MainEntity (
        id int8 not null,
        str varchar(255),
        primary key (id)
    );

    create table MainEntity_AUD (
        id int8 not null,
        REV int4 not null,
        REVTYPE int2,
        str varchar(255),
        str_MOD boolean,
        primary key (id, REV)
    );

    create table REVINFO (
        REV int4 not null,
        REVTSTMP int8,
        primary key (REV)
    );

    alter table MainEntity_AUD 
        add constraint FKdyho0e2yvr52e1nf5rt18k2ec 
        foreign key (REV) 
        references REVINFO;
