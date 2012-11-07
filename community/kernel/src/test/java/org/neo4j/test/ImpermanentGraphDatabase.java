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

package org.neo4j.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseSetting;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.IndexProvider;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.IdGeneratorFactory;
import org.neo4j.kernel.KernelExtension;
import org.neo4j.kernel.impl.cache.CacheProvider;
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction;
import org.neo4j.kernel.impl.util.FileUtils;
import org.neo4j.kernel.logging.ClassicLoggingService;
import org.neo4j.kernel.logging.Logging;
import org.neo4j.test.impl.EphemeralFileSystemAbstraction;
import org.neo4j.test.impl.EphemeralIdGenerator;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * A database meant to be used in unit tests. It will always be empty on start.
 */
public class ImpermanentGraphDatabase extends EmbeddedGraphDatabase
{
    private static final File PATH = new File( "target/test-data/impermanent-db" );
    private static final AtomicInteger ID = new AtomicInteger();
    private EphemeralFileSystemAbstraction fileSystemAbstraction;
    
    static
    {
        try
        {
            FileUtils.deleteRecursively( PATH );
        }
        catch ( IOException e )
        {
            throw new Error( "Couldn't clear directory", e );
        }
    }

    public ImpermanentGraphDatabase( Map<String, String> params )
    {
        super( path(), withoutMemmap( params ) );
    }

    public ImpermanentGraphDatabase( Map<String,String> params, Iterable<IndexProvider> indexProviders,
            Iterable<KernelExtension> kernelExtensions, Iterable<CacheProvider> cacheProviders )
    {
        super( path(), withoutMemmap( params ), indexProviders, kernelExtensions, cacheProviders );
    }

    @Override
    protected FileSystemAbstraction createFileSystemAbstraction()
    {
        return fileSystemAbstraction = new EphemeralFileSystemAbstraction();
    }

    @Override
    protected IdGeneratorFactory createIdGeneratorFactory()
    {
        return new EphemeralIdGenerator.Factory();
    }
    
    private static Map<String, String> withoutMemmap( Map<String, String> params )
    {   // Because EphemeralFileChannel doesn't support memorymapping
        Map<String, String> result = new HashMap<String, String>( params );
        result.put( GraphDatabaseSettings.use_memory_mapped_buffers.name(), GraphDatabaseSetting.BooleanSetting.FALSE );
        return result;
    }

    public ImpermanentGraphDatabase()
    {
        this( new HashMap<String, String>() );
    }
    
    @Override
    protected boolean isEphemeral()
    {
        return true;
    }
    
    @Override
    protected Logging createStringLogger()
    {
        ClassicLoggingService logging = new ClassicLoggingService( config );
        life.add( logging );
        return logging;
    }
    
    private static String path()
    {
        File path = null;
        do
        {
            path = new File( PATH, String.valueOf( ID.get() ) );
            if ( path.exists() ) ID.incrementAndGet();
        }
        while ( path.exists() );
        return path.getAbsolutePath();
    }

    private static void clearDirectory( File path )
    {
        try
        {
            FileUtils.deleteRecursively( path );
        }
        catch ( IOException e )
        {
            if ( GraphDatabaseSetting.osIsWindows() )
            {
                System.err.println( "Couldn't clear directory, and that's ok because this is Windows. Next " +
                        ImpermanentGraphDatabase.class.getSimpleName() + " will get a new directory" );
                e.printStackTrace();
                ID.incrementAndGet();
            }
            else
            {
                throw new RuntimeException( "Couldn't not clear directory" );
            }
        }
    }

    @Override
    public void shutdown()
    {
        super.shutdown();

        clearDirectory( new File( getStoreDir() ) );
    }

    public void cleanContent( boolean retainReferenceNode )
    {
        Transaction tx = beginTx();
        try
        {
            for ( Node node : GlobalGraphOperations.at( this ).getAllNodes() )
            {
                for ( Relationship rel : node.getRelationships( Direction.OUTGOING ) )
                {
                    rel.delete();
                }
                if ( !node.hasRelationship() )
                {
                    if ( retainReferenceNode )
                    {
                        try
                        {
                            Node referenceNode = getReferenceNode();
                            if ( !node.equals( referenceNode ) )
                            {
                                node.delete();
                            }
                        }
                        catch ( NotFoundException nfe )
                        {
                            // no ref node
                        }
                    }
                    else
                    {
                        node.delete();
                    }
                }
            }
            tx.success();
        }
        catch ( Exception e )
        {
            tx.failure();
        }
        finally
        {
            tx.finish();
        }
    }

    public void cleanContent()
    {
        cleanContent( false );
    }
}
