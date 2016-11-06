create sequence hibernate_sequence start with 1 increment by 1;

    create table ABTEILUNG (
        OID bigint not null,
        gender varchar(255),
        name varchar(255) not null,
        primary key (OID)
    );

    create table Employee (
        OID bigint not null,
        name varchar(255) not null,
        FK_department bigint,
        primary key (OID)
    );

    alter table Employee 
        add constraint FKps0mm7o60mrhle838yeh1u1rh 
        foreign key (FK_department) 
        references ABTEILUNG;
