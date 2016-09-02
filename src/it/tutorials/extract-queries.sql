--
-- Queries extraction file
--
-- ************************************************************
-- Get event by id.
-- Useful when loading event details
--
-- @name			eventById
-- @param			eventId		java.lang.Integer
--
-- @hql
--   SELECT
--   		  e.id
--   		FROM
--   		  org.hibernate.tutorial.hbm.Event e
--   		WHERE e.id = :eventId
--
-- ************************************************************

    select
        event0_.EVENT_ID as col_0_0_ 
    from
        EVENTS event0_ 
    where
        event0_.EVENT_ID=?;


-- ************************************************************
-- Delete an event
--
-- @name			deleteEvent
-- @param			eventId		java.lang.Integer
--
-- @hql
--   DELETE FROM
--   		  org.hibernate.tutorial.hbm.Event e
--   		WHERE e.id = :eventId
--
-- ************************************************************

    delete 
    from
        EVENTS 
    where
        EVENT_ID=?;


-- ************************************************************
-- A native query to get event by id
--
-- @name			nativeEventById
-- @param			eventId		java.lang.Integer
--
-- ************************************************************

    select
        EVENT_ID             
    from
        EVENTS             
    where
        EVENT_ID=:eventId;
