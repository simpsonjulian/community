/**
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.neo4j.examples;

import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.jmx.Kernel;
import org.neo4j.kernel.GraphDatabaseAPI;

import static org.junit.Assert.*;

public class JmxTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Test
    public void readJmxProperties()
    {
        GraphDatabaseService graphDbService = new GraphDatabaseFactory().newEmbeddedDatabase( temp.getRoot().getAbsolutePath());
        try
        {
            Date startTime = getStartTimeFromManagementBean( graphDbService );
            Date now = new Date();
            System.out.println( startTime + " " + now );
            assertTrue( startTime.before( now ) || startTime.equals( now ) );
        }
        finally
        {
            graphDbService.shutdown();
        }
    }

    // START SNIPPET: getStartTime
    private static Date getStartTimeFromManagementBean(
            GraphDatabaseService graphDbService )
    {
        GraphDatabaseAPI graphDb = (GraphDatabaseAPI) graphDbService;
        Kernel kernel = graphDb.getSingleManagementBean( Kernel.class );
        Date startTime = kernel.getKernelStartTime();
        return startTime;
    }
    // END SNIPPET: getStartTime
}
