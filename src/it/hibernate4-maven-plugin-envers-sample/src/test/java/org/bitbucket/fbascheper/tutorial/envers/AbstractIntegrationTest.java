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

import org.bitbucket.fbascheper.tutorial.envers.util.InitScriptRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DelegatingSmartContextLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Base class for the integration tests using Spring, Hibernate and JTA.
 *
 * @author Erik-Berndt Scheper
 * @since 11-09-2013
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        loader = DelegatingSmartContextLoader.class,
        locations = {"classpath:/hhv-test-datamodel-domain-context.xml", "classpath:/spring-persistence-context.xml"})
public abstract class AbstractIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Value(value = "${hibernate.dialect}")
    private String hibernateDialect;

    @Resource(name = "dataSource")
    private DataSource dataSource;

    @Inject
    private InitScriptRunner initScriptRunner;

    @Before
    public void initIntegrationTest() throws IOException {
        assertThat(transactionManager, notNullValue());
        assertThat(entityManager, notNullValue());
        assertThat(hibernateDialect, notNullValue());
        assertThat(dataSource, notNullValue());

        // re-run the database creation script for each test
        initScriptRunner.runScript();

    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected TransactionTemplate getTransactionTemplate() {

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template;
    }

}
