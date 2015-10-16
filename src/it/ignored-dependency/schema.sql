
    create sequence hibernate_sequence start 1 increment 1;

    create table MainEntity (
        id int8 not null,
        primary key (id)
    );
