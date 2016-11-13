create sequence hibernate_sequence start with 1 increment by 1;

    create table MyEntity (
        id integer not null,
        status blob,
        primary key (id)
    );
