
    alter table TTL_EVENT_AUD 
        drop constraint FK_cjsh8995uabmdm9b30uvmyj6p;

    drop table TTL_AUDIT_REVISION if exists;

    drop table TTL_EVENT if exists;

    drop table TTL_EVENT_AUD if exists;

    drop sequence TTL_AUDIT_REVISION_SEQ;

    drop sequence TTL_EVENT_SEQ;
