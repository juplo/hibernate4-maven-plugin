
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
        add constraint FK_kpx35pyi0ssiutbfxbf8klu06 
        foreign key (REV) 
        references REVINFO;

    create sequence hibernate_sequence;
