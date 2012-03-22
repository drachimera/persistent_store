/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.GridFS;

/**
 *
 * @author qvh
 */
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;

/**
* Implementation of PSGridFS v1.0
*
* <a href="http://www.mongodb.org/display/DOCS/GridFS+Specification">PSGridFS 1.0 spec</a>
*
* @dochub gridfs
*/
public class PSGridFS {

    /**
* file's chunk size
*/
    public static final int DEFAULT_CHUNKSIZE = 256 * 1024;

    /**
* file's max chunk size
*/
    public static final long MAX_CHUNKSIZE = (long)(3.5 * 1000 * 1000);

    /**
* bucket to use for the collection namespaces
*/
    public static final String DEFAULT_BUCKET = "fs";

    // --------------------------
    // ------ constructors -------
    // --------------------------

    /**
* Creates a PSGridFS instance for the default bucket "fs"
* in the given database.
*
* @param db database to work with
*/
    public PSGridFS(DB db) {
        this(db, DEFAULT_BUCKET);
    }

    /**
* Creates a PSGridFS instance for the specified bucket
* in the given database.
*
* @param db database to work with
* @param bucket bucket to use in the given database
*/
    public PSGridFS(DB db, String bucket) {
        _db = db;
        _bucketName = bucket;

        _filesCollection = _db.getCollection( _bucketName + ".files" );
        _chunkCollection = _db.getCollection( _bucketName + ".chunks" );

        // ensure standard indexes as long as collections are small
        if (_filesCollection.count() < 1000)
            _filesCollection.ensureIndex( BasicDBObjectBuilder.start().add( "filename" , 1 ).add( "uploadDate" , 1 ).get() );
        if (_chunkCollection.count() < 1000)
            _chunkCollection.ensureIndex( BasicDBObjectBuilder.start().add( "files_id" , 1 ).add( "n" , 1 ).get() );

        _filesCollection.setObjectClass( PSGridFSDBFile.class );
    }


    // --------------------------
    // ------ utils -------
    // --------------------------


    /**
* gets the list of files stored in this gridfs, sorted by filename
*
* @return cursor of file objects
*/
    public DBCursor getFileList(){
        return _filesCollection.find().sort(new BasicDBObject("filename",1));
    }

    /**
* gets a filtered list of files stored in this gridfs, sorted by filename
*
* @param query filter to apply
* @return cursor of file objects
*/
    public DBCursor getFileList( DBObject query ){
        return _filesCollection.find( query ).sort(new BasicDBObject("filename",1));
    }


    // --------------------------
    // ------ reading -------
    // --------------------------

    /**
* finds one file matching the given id. Equivalent to findOne(id)
* @param id
* @return
*/
    public PSGridFSDBFile find( ObjectId id ){
        return findOne( id );
    }
    /**
* finds one file matching the given id.
* @param id
* @return
*/
    public PSGridFSDBFile findOne( ObjectId id ){
        return findOne( new BasicDBObject( "_id" , id ) );
    }
    /**
* finds one file matching the given filename
* @param filename
* @return
*/
    public PSGridFSDBFile findOne( String filename ){
        return findOne( new BasicDBObject( "filename" , filename ) );
    }
    /**
* finds one file matching the given query
* @param query
* @return
*/
    public PSGridFSDBFile findOne( DBObject query ){
        return _fix( _filesCollection.findOne( query ) );
    }

    /**
* finds a list of files matching the given filename
* @param filename
* @return
*/
    public List<PSGridFSDBFile> find( String filename ){
        return find( new BasicDBObject( "filename" , filename ) );
    }
    /**
* finds a list of files matching the given query
* @param query
* @return
*/
    public List<PSGridFSDBFile> find( DBObject query ){
        List<PSGridFSDBFile> files = new ArrayList<PSGridFSDBFile>();

        DBCursor c = _filesCollection.find( query );
        while ( c.hasNext() ){
            files.add( _fix( c.next() ) );
        }
        return files;
    }

    private PSGridFSDBFile _fix( Object o ){
        if ( o == null )
            return null;

        if ( ! ( o instanceof PSGridFSDBFile ) )
            throw new RuntimeException( "somehow didn't get a GridFSDBFile" );

        PSGridFSDBFile f = (PSGridFSDBFile)o;
        f._fs = this;
        return f;
    }


    // --------------------------
    // ------ remove -------
    // --------------------------

    /**
* removes the file matching the given id
* @param id
*/
    public void remove( ObjectId id ){
        _filesCollection.remove( new BasicDBObject( "_id" , id ) );
        _chunkCollection.remove( new BasicDBObject( "files_id" , id ) );
    }

    /**
* removes all files matching the given filename
* @param filename
*/
    public void remove( String filename ){
        remove( new BasicDBObject( "filename" , filename ) );
    }

    /**
* removes all files matching the given query
* @param query
*/
    public void remove( DBObject query ){
        for ( PSGridFSDBFile f : find( query ) ){
            f.remove();
        }
    }


    // --------------------------
    // ------ writing -------
    // --------------------------
    
    public PSGridNIO createNIOFile(){
        return new PSGridNIO(this);
    }

    /**
* creates a file entry.
* After calling this method, you have to call save() on the PSGridFSInputFile file
* @param data the file's data
* @return
*/
    public PSGridFSInputFile createFile( byte[] data ){
        return createFile( new ByteArrayInputStream( data ), true );
    }


    /**
* creates a file entry.
* After calling this method, you have to call save() on the PSGridFSInputFile file
* @param f the file object
* @return
* @throws IOException
*/
    public PSGridFSInputFile createFile( File f )
        throws IOException {
        return createFile( new FileInputStream( f ) , f.getName(), true );
    }

    /**
* creates a file entry.
* after calling this method, you have to call save() on the PSGridFSInputFile file
* @param in an inputstream containing the file's data
* @return
*/
    public PSGridFSInputFile createFile( InputStream in ){
        return createFile( in , null );
    }

    /**
* creates a file entry.
* after calling this method, you have to call save() on the PSGridFSInputFile file
* @param in an inputstream containing the file's data
* @param closeStreamOnPersist indicate the passed in input stream should be closed
* once the data chunk persisted
* @return
*/
    public PSGridFSInputFile createFile( InputStream in, boolean closeStreamOnPersist ){
        return createFile( in , null, closeStreamOnPersist );
    }

    /**
* creates a file entry.
* After calling this method, you have to call save() on the PSGridFSInputFile file
* @param in an inputstream containing the file's data
* @param filename the file name as stored in the db
* @return
*/
    public PSGridFSInputFile createFile( InputStream in , String filename ){
        return new PSGridFSInputFile( this , in , filename );
    }

    /**
* creates a file entry.
* After calling this method, you have to call save() on the PSGridFSInputFile file
* @param in an inputstream containing the file's data
* @param filename the file name as stored in the db
* @param closeStreamOnPersist indicate the passed in input stream should be closed
* once the data chunk persisted
* @return
*/
    public PSGridFSInputFile createFile( InputStream in , String filename, boolean closeStreamOnPersist ){
        return new PSGridFSInputFile( this , in , filename, closeStreamOnPersist );
    }

    /**
* @see {@link PSGridFS#createFile()} on how to use this method
* @param filename the file name as stored in the db
* @return
*/
    public PSGridFSInputFile createFile(String filename) {
        return new PSGridFSInputFile( this , filename );
    }

    /**
* This method creates an empty {@link PSGridFSInputFile} instance. On this
* instance an {@link java.io.OutputStream} can be obtained using the
* {@link PSGridFSInputFile#getOutputStream()} method. You can still call
* {@link PSGridFSInputFile#setContentType(String)} and
* {@link PSGridFSInputFile#setFilename(String)}. The file will be completely
* written and closed after calling the {@link java.io.OutputStream#close()}
* method on the output stream.
*
* @return PSGridFS file handle instance.
*/
    public PSGridFSInputFile createFile() {
        return new PSGridFSInputFile( this );
    }



    // --------------------------
    // ------ members -------
    // --------------------------

    /**
* gets the bucket name used in the collection's namespace
* @return
*/
    public String getBucketName(){
        return _bucketName;
    }

    /**
* gets the db used
* @return
*/
    public DB getDB(){
        return _db;
    }

    protected final DB _db;
    protected final String _bucketName;
    protected final DBCollection _filesCollection;
    protected final DBCollection _chunkCollection;

}