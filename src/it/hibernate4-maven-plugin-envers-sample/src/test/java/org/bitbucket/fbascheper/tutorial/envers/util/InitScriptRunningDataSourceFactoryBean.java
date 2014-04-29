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

import javax.sql.DataSource;

/**
 * A Bean of a {@link javax.sql.DataSource} type that initially runs a database creation script.
 *
 * @author Erik-Berndt Scheper
 * @since 11-09-2013
 */
public class InitScriptRunningDataSourceFactoryBean {

    /**
     * Factory method returning a DataSource after running the init script.
     *
     * @param initScriptRunner the script runner implementation
     */
    public static DataSource dataSourceFactory(InitScriptRunner initScriptRunner) {

        initScriptRunner.runScript();

        return initScriptRunner.getDataSource();
    }

}
