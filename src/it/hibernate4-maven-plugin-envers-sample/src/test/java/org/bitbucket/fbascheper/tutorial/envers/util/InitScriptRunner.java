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
package org.bitbucket.fbascheper.tutorial.envers.util;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;

/**
 * A initialization script runner.
 *
 * @author Erik-Berndt Scheper
 * @since 11-09-2013
 */
public interface InitScriptRunner {

    /**
     * Run the database creation script.
     */
    public void runScript();

    /**
     * Return the datasource used to run the script
     *
     * @return the datasource
     */
    public DataSource getDataSource();


    /**
     * Default implementation for the runner of the creation script.
     */
    public static class InitScriptRunnerImpl implements InitScriptRunner {

        private final DataSource dataSource;
        private final Resource dbDropScriptLocation;
        private final Resource dbCreateScriptLocation;


        /**
         * Initializing constructor.
         *
         * @param dataSource             the raw {@link javax.sql.DataSource} to return
         * @param dbDropScriptLocation   location of the create DB drop script to run
         * @param dbCreateScriptLocation location of the create DB create script to run
         */
        public InitScriptRunnerImpl(DataSource dataSource,
                                    Resource dbDropScriptLocation,
                                    Resource dbCreateScriptLocation) {
            this.dataSource = dataSource;
            this.dbDropScriptLocation = dbDropScriptLocation;
            this.dbCreateScriptLocation = dbCreateScriptLocation;
        }

        @Override
        public void runScript() {

            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            try {
                JdbcTestUtils.executeSqlScript(jdbcTemplate, dbDropScriptLocation, false);
            }
            catch (Exception e) {}
            try {
            JdbcTestUtils.executeSqlScript(jdbcTemplate, dbCreateScriptLocation, false);
            }
            catch (Exception e) {}

        }

        @Override
        public DataSource getDataSource() {

            return this.dataSource;

        }
    }

}
