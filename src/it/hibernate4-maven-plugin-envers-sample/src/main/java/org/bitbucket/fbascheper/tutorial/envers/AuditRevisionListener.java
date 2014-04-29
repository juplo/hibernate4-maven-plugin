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

import org.hibernate.envers.RevisionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The {@link org.hibernate.envers.RevisionListener}-implementation used to initialize an {@link AuditRevision}.
 *
 * @author Erik-Berndt Scheper
 * @see AuditRevision
 * @since 11-09-2013
 */
public class AuditRevisionListener implements RevisionListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void newRevision(Object revisionEntity) {

        AuditRevision auditRevision = (AuditRevision) revisionEntity;
        auditRevision.setRevisionTimeStamp(new Date());

        auditRevision.setUserName("EXAMPLE_USER");


    }

}
