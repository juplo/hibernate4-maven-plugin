/**
 * Copyright 2013 F.B.A. Scheper.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. *
 */
package org.bitbucket.fbascheper.tutorial.envers;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * Illustrates the set up and use of Envers using JTA; based on the original hibernate tutorial by Steve Ebersole.
 *
 * @author Erik-Berndt Scheper
 * @since 11-09-2013
 */
public class EnversIllustrationTest extends AbstractIntegrationTest {

    @Test
    public void testOne() {

        // create a couple of events
        final Event event1 = getTransactionTemplate().execute(new TransactionCallback<Event>() {
            @Override
            public Event doInTransaction(TransactionStatus status) {
                // revision 1
                Event event = new Event("Our very first event!", new Date());
                getEntityManager().persist(event);
                return event;

            }
        });
        final Event event2 = getTransactionTemplate().execute(new TransactionCallback<Event>() {
            @Override
            public Event doInTransaction(TransactionStatus status) {
                // revision 2
                Event event = new Event("A follow up event", new Date());
                getEntityManager().persist(event);
                return event;

            }
        });

        // now lets pull events from the database and list them

        List<Event> result = getTransactionTemplate().execute(new TransactionCallback<List<Event>>() {
            @Override
            public List<Event> doInTransaction(TransactionStatus status) {
                List<Event> result = getEntityManager().createQuery("select evt from Event evt", Event.class).getResultList();
                for (Event event : result) {
                    System.out.println("Event (" + event.getDate() + ") : " + event.getTitle());
                }

                return result;
            }
        });

        // verify that id's were generated
        final Long event1Id = event1.getId();
        final Long event2Id = event2.getId();

        assertThat(event1Id, notNullValue());
        assertThat(event2Id, notNullValue());

        // so far the code is the same as we have seen in previous tutorials.  Now lets leverage Envers...
        // first lets create some revisions
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // revision 3
                Event myEvent = getEntityManager().find(Event.class, event2Id);
                myEvent.setDate(new Date());
                myEvent.setTitle(myEvent.getTitle() + " (rescheduled)");

            }
        });

        // and then use an AuditReader to look back through history
        getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {

                Event myEvent = getEntityManager().find(Event.class, event2Id);
                assertThat("A follow up event (rescheduled)", is(myEvent.getTitle()));

                AuditReader reader = AuditReaderFactory.get(getEntityManager());

                List<? extends Number> event2Revisions = reader.getRevisions(Event.class, event2Id);
                assertThat(event2Revisions.size(), is(2));

                long event2Revision1 = event2Revisions.get(0).longValue();
                long event2Revision2 = event2Revisions.get(1).longValue();

                assertThat(event2Revision1, is(2L));
                assertThat(event2Revision2, is(3L));

                Event firstRevision = reader.find(Event.class, event2Id, event2Revision1);

                assertThat(firstRevision, notNullValue());
                assertThat(firstRevision.getTitle(), notNullValue());
                assertThat(firstRevision.getTitle(), not(is(myEvent.getTitle())));
                assertThat(firstRevision.getDate(), not(is(myEvent.getDate())));

                Event secondRevision = reader.find(Event.class, event2Id, event2Revision2);
                assertThat(secondRevision, notNullValue());
                assertThat(secondRevision.getTitle(), notNullValue());
                assertThat(secondRevision.getTitle(), is(myEvent.getTitle()));
                assertThat(secondRevision.getDate(), is(myEvent.getDate()));

            }

        });

    }

}
