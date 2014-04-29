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

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

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
import java.io.Serializable;
import java.util.Date;

/**
 * The implementation of the {@link org.hibernate.envers.RevisionEntity}.
 *
 * @author Erik-Berndt Scheper
 * @see org.hibernate.envers.RevisionEntity
 * @since 11-09-2013
 */
@Entity
@Table(name = "TTL_AUDIT_REVISION")
@RevisionEntity(AuditRevisionListener.class)
public class AuditRevision implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = "TTL_AUDIT_REVISION_SEQ", sequenceName = "TTL_AUDIT_REVISION_SEQ", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TTL_AUDIT_REVISION_SEQ")
    @RevisionNumber
    private Long id;

    @RevisionTimestamp
    @Column(name = "ENVERS_TSTAMP", nullable = false)
    private long timestamp;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EVENT_DATE")
    private Date revisionTimeStamp;

    @NotNull
    @Column(name = "USER_NAME", length = 80, nullable = false)
    private String userName;

    // ********************** Getters and setters ********************** //

    /**
     * @return the id of this revision
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id of this revision
     */
    void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the timestamp of this revision
     */
    long getTimestamp() {
        return timestamp;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the timestamp of this revision
     */
    public Date getRevisionTimeStamp() {
        return revisionTimeStamp;
    }

    void setRevisionTimeStamp(Date revisionTimeStamp) {
        this.revisionTimeStamp = revisionTimeStamp;
    }

    /**
     * @return name of the user who initiated the change resulting in this revision.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName name of the user who initiated the change resulting in this revision.
     */
    void setUserName(String userName) {
        this.userName = userName;
    }

    // ********************** Common Methods ********************** //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuditRevision that = (AuditRevision) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        output.append("AuditRevision {");
        output.append(" id = \"").append(getId()).append("\", ");
        output.append(" revisionTimeStamp = \"").append(revisionTimeStamp).append("\", ");
        output.append(" userName = \"").append(userName).append("\"}");

        return output.toString();
    }

}
