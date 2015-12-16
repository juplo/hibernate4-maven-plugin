
    alter table TTL_EVENT_AUD 
        drop constraint FK295td34se5kaxwgo8i1ph0k4;

    drop table TTL_AUDIT_REVISION if exists;

    drop table TTL_EVENT if exists;

    drop table TTL_EVENT_AUD if exists;

    drop sequence TTL_AUDIT_REVISION_SEQ;

    drop sequence TTL_EVENT_SEQ;
