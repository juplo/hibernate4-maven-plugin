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

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionNumber;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * A random event registered, based on the original hibernate tutorial by Steve Ebersole.
 *
 * @author Erik-Berndt Scheper
 * @see AuditRevision
 * @since 11-09-2013
 */
@Entity
@Table(name = "TTL_EVENT")
@Audited
public class Event {

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = "TTL_EVENT_SEQ", sequenceName = "TTL_EVENT_SEQ", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TTL_EVENT_SEQ")
    @RevisionNumber
    private Long id;

    @NotNull
    @Column(name = "TITLE", length = 80, nullable = false)
    private String title;

    @NotNull
    @Column(name = "EVENT_DATE", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    /**
     * Default constructor, mandated by JPA.
     */
    Event() {
        // do nothing
    }

    /**
     * Initializing constructor.
     *
     * @param title title of the event
     * @param date  date of the event
     */
    public Event(String title, Date date) {
        this.title = title;
        this.date = date;
    }

    // ********************** Getters and setters ********************** //

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // ********************** Common Methods ********************** //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event event = (Event) o;

        if (date != null ? !date.equals(event.date) : event.date != null) return false;
        if (title != null ? !title.equals(event.title) : event.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        output.append("Event {");
        output.append(" id = \"").append(getId()).append("\", ");
        output.append(" title = \"").append(title).append("\", ");
        output.append(" date = \"").append(date).append("\"}");

        return output.toString();
    }


}