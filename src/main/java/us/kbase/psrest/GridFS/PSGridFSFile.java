/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.GridFS;

/**
 *
 * @author qvh
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.BSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFSFile;
import com.mongodb.util.JSON;

/**
 * 
 * Daniel Quest: These files are needed because they have to be modified to allow for 
 * streaming data APIs
* The abstract class representing a PSGridFS file
* @author antoine
*/
public class PSGridFSFile extends GridFSFile {

    
    // ------------------------------
    // --------- db -------
    // ------------------------------

    /**
* Saves the file entry to the files collection
*/
    public void save(){
        if ( _fs == null )
            throw new MongoException( "need _fs" );
        _fs._filesCollection.save( this );
    }

    /**
* Verifies that the MD5 matches between the database and the local file.
* This should be called after transferring a file.
* @throws MongoException
*/
    public void validate() throws MongoException {
        if ( _fs == null )
            throw new MongoException( "no _fs" );
        if ( _md5 == null )
            throw new MongoException( "no _md5 stored" );
        
        DBObject cmd = new BasicDBObject( "filemd5" , _id );
        cmd.put( "root" , _fs._bucketName );
        DBObject res = _fs._db.command( cmd );
        if ( res != null && res.containsField( "md5" ) ) {
            String m = res.get( "md5" ).toString();
            if ( m.equals( _md5 ) )
                return;
            throw new MongoException( "md5 differ. mine [" + _md5 + "] theirs [" + m + "]" );
        }

        // no md5 from the server
        throw new MongoException( "no md5 returned from server: " + res );

    }

    /**
* Returns the number of chunks that store the file data
* @return
*/
    public int numChunks(){
        double d = _length;
        d = d / _chunkSize;
        return (int)Math.ceil( d );
    }

    // ------------------------------
    // --------- getters -------
    // ------------------------------


    /**
* Gets the id
* @return
*/
    public Object getId(){
        return _id;
    }

    /**
* Gets the filename
* @return
*/
    public String getFilename(){
        return _filename;
    }

    /**
* Gets the content type
* @return
*/
    public String getContentType(){
        return _contentType;
    }

    /**
* Gets the file's length
* @return
*/
    public long getLength(){
        return _length;
    }
    
    /**
* Gets the size of a chunk
* @return
*/
    public long getChunkSize(){
        return _chunkSize;
    }
    
    /**
* Gets the upload date
* @return
*/
    public Date getUploadDate(){
        return _uploadDate;
    }

    /**
* Gets the aliases from the metadata.
* note: to set aliases, call put( "aliases" , List<String> )
* @return
*/
    @SuppressWarnings("unchecked")
    public List<String> getAliases(){
        return (List<String>)_extradata.get( "aliases" );
    }

    /**
* Gets the file metadata
* @return
*/
    public DBObject getMetaData(){
        return (DBObject)_extradata.get( "metadata" );
    }

    /**
* Gets the file metadata
* @return
*/
    public void setMetaData(DBObject metadata){
        _extradata.put( "metadata", metadata );
    }

    /**
* Gets the observed MD5 during transfer
* @return
*/
    public String getMD5(){
        return _md5;
    }

    // ------------------------------
    // --------- DBOBject methods ---
    // ------------------------------
    
    public Object put( String key , Object v ){
        if ( key == null )
            throw new RuntimeException( "key should never be null" );
        else if ( key.equals( "_id" ) )
            _id = v;
        else if ( key.equals( "filename" ) )
            _filename = v == null ? null : v.toString();
        else if ( key.equals( "contentType" ) )
            _contentType = (String)v;
        else if ( key.equals( "length" ) )
            _length = ((Number)v).longValue();
        else if ( key.equals( "chunkSize" ) )
            _chunkSize = ((Number)v).longValue();
        else if ( key.equals( "uploadDate" ) )
            _uploadDate = (Date)v;
        else if ( key.equals( "md5" ) )
            _md5 = (String)v;
        else
            _extradata.put( key , v );
        return v;
    }

    @Override
    public Object get( String key ){
        if ( key == null )
            throw new RuntimeException( "key should never be null" );
        else if ( key.equals( "_id" ) )
            return _id;
        else if ( key.equals( "filename" ) )
            return _filename;
        else if ( key.equals( "contentType" ) )
            return _contentType;
        else if ( key.equals( "length" ) )
            return _length;
        else if ( key.equals( "chunkSize" ) )
            return _chunkSize;
        else if ( key.equals( "uploadDate" ) )
            return _uploadDate;
        else if ( key.equals( "md5" ) )
            return _md5;
        return _extradata.get( key );
    }

    public void putAll( BSONObject o ){
        throw new UnsupportedOperationException();
    }

    public void putAll( Map m ){
        throw new UnsupportedOperationException();
    }

    public Map toMap(){
        throw new UnsupportedOperationException();
    }

    public Object removeField( String key ){
        throw new UnsupportedOperationException();
    }

    /*
* @deprecated
*/
    @Deprecated
    @Override
    public boolean containsKey( String s ){
        return containsField( s );
    }

    @Override
    public boolean containsField(String s){
        return keySet().contains( s );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> keySet(){
        Set<String> keys = new HashSet();
        keys.addAll(VALID_FIELDS);
        keys.addAll(_extradata.keySet());
        return keys;
    }

    @Override
    public boolean isPartialObject(){
        return false;
    }

    @Override
    public void markAsPartialObject(){
        throw new RuntimeException( "can't load partial GridFSFile file" );
    }
    
    // ----------------------
    // ------- fields -------
    // ----------------------

    @Override
    public String toString(){
        return JSON.serialize( this );
    }

    /**
* Sets the PSGridFS associated with this file
* @param fs
*/
    protected void setGridFS( PSGridFS fs ){
        _fs = fs;
    }

    protected PSGridFS _fs = null;

    Object _id;
    String _filename;
    String _contentType;
    long _length;
    long _chunkSize;
    Date _uploadDate;
    List<String> _aliases;
    DBObject _extradata = new BasicDBObject();
    String _md5;

    @SuppressWarnings("unchecked")
    final static Set<String> VALID_FIELDS = Collections.unmodifiableSet( new HashSet( Arrays.asList( new String[]{
                    "_id" , "filename" , "contentType" , "length" , "chunkSize" ,
                    "uploadDate" , "aliases" , "md5"
                } ) ) );
}