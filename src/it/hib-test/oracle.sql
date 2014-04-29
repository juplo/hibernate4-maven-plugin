
    drop table test_simple cascade constraints;

    create table test_simple (
        uuid varchar2(36 char) not null,
        content clob,
        externalid varchar2(148 char),
        sources varchar2(255 char),
        primary key (uuid)
    );

    create index idx_test_simple_tuple on test_simple (sources, uuid);
