/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.metatest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.kernel.impl.annotations.Documented;
import org.neo4j.test.*;
import org.neo4j.test.GraphDescription.Graph;

public class TestJavaTestDocsGenerator implements GraphHolder
{
    private static GraphDatabaseService graphdb;
    public @Rule
    TestData<Map<String, Node>> data = TestData.producedThrough( GraphDescription.createGraphFor(
            this, true ) );

    public @Rule
    TestData<JavaTestDocsGenerator> gen = TestData.producedThrough( JavaTestDocsGenerator.PRODUCER );

    File directory = TargetDirectory.forTest(getClass()).directory("testdocs", false);
    String sectionName = "testsection";
    File sectionDirectory = new File(directory, sectionName);

    @Documented( value = "Title1.\n\nhej\n@@snippet1\n\nmore docs\n@@snippet_2-1\n@@snippet12\n." )
    @Test
    @Graph( "I know you" )
    public void can_create_docs_from_method_name() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.setGraph( graphdb );
        assertNotNull( data.get().get( "I" ) );
        String snippet1 = "snippet1-value";
        String snippet12 = "snippet12-value";
        String snippet2 = "snippet2-value";
        doc.addSnippet( "snippet1", snippet1 );
        doc.addSnippet( "snippet12", snippet12 );
        doc.addSnippet( "snippet_2-1", snippet2 );
        doc.document( directory.getAbsolutePath(), sectionName );
        String result = readFileAsString( new File(sectionDirectory, "title1.txt"));
        assertTrue( result.contains( snippet1 ) );
        assertTrue( result.contains( snippet12 ) );
        assertTrue( result.contains( snippet2 ) );
    }

    @Documented( value = "@@snippet1\n" )
    @Test
    @Graph( "I know you" )
    public void will_not_complain_about_missing_snippets() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.document( directory.getAbsolutePath(), sectionName );
    }

    /**
     * Title2.
     * 
     * @@snippet1
     * 
     *            more stuff
     * 
     * 
     * @@snippet2
     */
    @Documented
    @Test
    @Graph( "I know you" )
    public void canCreateDocsFromSnippetsInAnnotations() throws Exception
    {
        data.get();
        JavaTestDocsGenerator doc = gen.get();
        doc.setGraph( graphdb );
        assertNotNull( data.get().get( "I" ) );
        String snippet1 = "snippet1-value";
        String snippet2 = "snippet2-value";
        doc.addSnippet( "snippet1", snippet1 );
        doc.addSnippet( "snippet2", snippet2 );
        doc.document( directory.getAbsolutePath(), sectionName );
        String result = readFileAsString( new File(sectionDirectory, "title2.txt"));
        assertTrue( result.contains( snippet1 ) );
        assertTrue( result.contains( snippet2 ) );
    }

    private static String readFileAsString( File file )
            throws java.io.IOException
    {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = new BufferedInputStream( new FileInputStream(
                file ) );
        f.read( buffer );
        return new String( buffer );
    }

    @Override
    public GraphDatabaseService graphdb()
    {
        return graphdb;
    }

    @BeforeClass
    public static void setUp()
    {
        graphdb = new ImpermanentGraphDatabase();
    }
    
    @AfterClass
    public static void shutdown()
    {
        try
        {
            if ( graphdb != null ) graphdb.shutdown();
        }
        finally
        {
            graphdb = null;
        }
    }
}
