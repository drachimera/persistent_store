/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.GridFS;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Date;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.util.SimplePool;
import com.mongodb.util.Util;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import us.kbase.psrest.util.StreamingDataPocketKnife;

/**
 *
 * @author qvh
 * This class is sort of like PSGridFSInputFile, but it is much more manual, you have to do the right things
 * and do them in order.  This works by passing buffers of the correct size directly to GridFS inserts
 * -- use it carefully! it assumes you know what you are doing.
 * 
 */
public class PSGridNIO extends PSGridFSFile {
    
/**
* Default constructor setting the GridFS file name and providing an input
* stream containing data to be written to the file.
*
* @param fs
* The GridFS connection handle.
* @param filename
* Name of the file to be created.
* @param closeStreamOnPersist
indicate the passed in input stream should be closed once the data chunk persisted
*/
    PSGridNIO( PSGridFS fs , String filename, boolean closeStreamOnPersist ) {
        _fs = fs;
        _filename = filename;
        _closeStreamOnPersist = closeStreamOnPersist;

        _id = new ObjectId();
        _chunkSize = GridFS.DEFAULT_CHUNKSIZE;
        _uploadDate = new Date();
        _messageDigester = _md5Pool.get();
        _messageDigester.reset();
        //_buffer = new byte[(int) _chunkSize];
    }

    /**
* Default constructor setting the GridFS file name and providing an input
* stream containing data to be written to the file.
*
* @param fs
* The GridFS connection handle.
* @param in
* Stream used for reading data from.
* @param filename
* Name of the file to be created.
*/
    PSGridNIO( PSGridFS fs , String filename ) {
        this( fs, filename, false);
    }

    /**
* Minimal constructor that does not rely on the presence of an
* {@link java.io.InputStream}. An {@link java.io.OutputStream} can later be
* obtained for writing using the {@link #getOutputStream()} method.
*
* @param fs
* The GridFS connection handle.
*/
    PSGridNIO( PSGridFS fs ) {
        this( fs , null , false );
    }

    /**
* Sets the file name on the GridFS entry.
*
* @param fn
* File name.
*/
    public void setFilename( String fn ) {
        _filename = fn;
    }

    /**
* Sets the content type (MIME type) on the GridFS entry.
*
* @param ct
* Content type.
*/
    public void setContentType( String ct ) {
        _contentType = ct;
    }

    /**
* Set the chunk size. This must be called before saving any data.
* @param _chunkSize
*/
    public void setChunkSize(long _chunkSize) {
        if (_savedChunks)
            return;
        this._chunkSize = _chunkSize;
    }
    


    /**
* calls {@link PSGridFSInputFile#save(long)} with the existing chunk size
*/
//    @Override
//    public void save() {
//        save( _chunkSize);
//    }

    /**
* This method first calls saveChunks(long) if the file data has not been saved yet.
* Then it persists the file entry to GridFS.
*
* @param chunkSize
* Size of chunks for file in bytes.
*/
//    public void save( long chunkSize ) {
//        if (_outputStream != null)
//            throw new MongoException( "cannot mix OutputStream and regular save()" );
//
//        // note that chunkSize only changes _chunkSize in case we actually save chunks
//        // otherwise there is a risk file and chunks are not compatible
//        if ( ! _savedChunks ) {
//            try {
//                saveChunks( chunkSize );
//            } catch ( IOException ioe ) {
//                throw new MongoException( "couldn't save chunks" , ioe );
//            }
//        }
//
//        super.save();
//    }

    /**
* @see com.mongodb.gridfs.PSGridFSInputFile#saveChunks(long)
*
* @return Number of the next chunk.
* @throws IOException
* on problems reading the new entry's
* {@link java.io.InputStream}.
*/


    /**
* Saves all data from byte array into chunks 
* to GridFS. A non-default chunk size can be specified.
* This method does NOT save the file object itself, one must call save() to do so.
*
* @param chunkSize
* Size of chunks for file in bytes.
* @return Number of the next chunk.
* @throws IOException
* on problems reading the new entry's
* {@link java.io.InputStream}.
*/
//    public int saveChunks( long chunkSize ) throws IOException {
//        if (_outputStream != null)
//            throw new MongoException( "cannot mix OutputStream and regular save()" );
//        if ( _savedChunks )
//            throw new MongoException( "chunks already saved!" );
//
//        if ( chunkSize <= 0 || chunkSize > PSGridFS.MAX_CHUNKSIZE ) {
//            throw new MongoException( "chunkSize must be greater than zero and less than or equal to GridFS.MAX_CHUNKSIZE");
//        }
//
//        if ( _chunkSize != chunkSize ) {
//            _chunkSize = chunkSize;
//            _buffer = new byte[(int) _chunkSize];
//        }
//
//        int bytesRead = 0;
//        while ( bytesRead >= 0 ) {
//            _currentBufferPosition = 0;
//            bytesRead = _readStream2Buffer();
//            _dumpBuffer( true );
//        }
//
//        // only finish data, do not write file, in case one wants to change metadata
//        _finishData();
//        return _currentChunkNumber;
//    }
    
    public DBObject saveSingleChunk(String s) throws IOException{
        System.out.println("saveSingleChunkString: ");
        System.out.println(s);
        ByteBuffer bb = ByteBuffer.allocate(s.length());
        System.out.println("buffer allocated");
        //System.arraycopy(s.getBytes(), 0, bb.array(), 0, s.length());
        for(int i = 0; i< s.length(); i++){
            bb.put((byte) s.charAt(i));
        }
        System.out.println("buffer copied");
        this.printBuffer(bb);
        DBObject saveSingleChunk = saveSingleChunk(bb);
        return saveSingleChunk;
    }
    
    public DBObject flush(){
        return _dumpBuffer();
    }
    
    private int bytesRead = 0;
    public DBObject saveSingleChunk(ByteBuffer byteBuffer) throws IOException {
        System.out.println("saveSingleChunk");
        byteBuffer.flip();//switch buffer to read mode.
        bytesRead += byteBuffer.limit();
        System.out.println("bytesRead: " + bytesRead);
        if(_buffer == null){
            _buffer = ByteBuffer.allocate((int) this._chunkSize);
        }
        //This method assumes that byteBuffer.limit < chunkSize
        if(byteBuffer.limit() > this._chunkSize){
            throw new MongoException( "ByteBuffers can't be larger than chunkSize!" );
        }
        //When we try to save buf, there are 2 possibilities:
        //A: The buf + _buffer is smaller than what we need for a chunk - append it to the chunk and keep going
        if( byteBuffer.limit() + _buffer.limit() < this._chunkSize ){
            System.out.println("saveSingleChunk case A: ");
            _buffer.put(byteBuffer);
            byteBuffer.clear(); //make it ready for writing.
            return null;
        }
        //B: The buf + _buffer is large enough to create a save; create a new chunk by filling _buffer to capacity, dump _buffer and fill the remaining
        //contents from buf into _buffer
        else {
            System.out.println("saveSingleChunk case B: ");
            DBObject dbo = null;
            //fill _buffer to capacity
            while(byteBuffer.hasRemaining()){
                _buffer.put(byteBuffer.get());
                if(_buffer.position() == _buffer.capacity()){
                    System.err.println("Try to dump buffer");
                    printBuffer(_buffer);
                    dbo = _dumpBuffer();
                }
            }           
            return dbo;
        }
         
        
        
//        int chunkSize = buf.length;
//        if ( chunkSize <= 0 || chunkSize > PSGridFS.MAX_CHUNKSIZE ) {
//            throw new MongoException( "chunkSize must be greater than zero and less than or equal to GridFS.MAX_CHUNKSIZE");
//        }
//        _savedChunks = true;
//        if ( _chunkSize != chunkSize ) {
//            throw new MongoException( "buffer passed to PSGridNIO must be the same size as GridFS chunksize");
//        }
//        _currentBufferPosition = 0;


    }


    /**
* Dumps a new chunk into the chunks collection. Depending on the flag, also
* partial buffers (at the end) are going to be written immediately.
*
* @param data
* Data for chunk.
* @param writePartial
* Write also partial buffers full.
*/
    private DBObject _dumpBuffer( ) {
        System.out.println("_dumpBuffer");
        if (_buffer.limit() == 0) {
            // chunk is empty, may be last chunk
            System.out.println("chunk is empty");
            return null;
        }
        byte[] array = _buffer.array();
        DBObject chunk = BasicDBObjectBuilder.start()
                .add( "files_id", _id )
                .add( "n", _currentChunkNumber )
                .add( "data", array ).get();
        _fs._chunkCollection.save( chunk );
        _currentChunkNumber++;
        _totalBytes += _buffer.limit();
        _messageDigester.update( array );
        _currentBufferPosition = 0;
        System.out.println("totalBytes saved: " + _totalBytes);
        return chunk;
    }

    /**
* Reads a buffer full from the {@link java.io.InputStream}.
*
* @return Number of bytes read from stream.
* @throws IOException
* if the reading from the stream fails.
*/
//    private int _readStream2Buffer() throws IOException {
//        int bytesRead = 0;
//        while ( _currentBufferPosition < _chunkSize && bytesRead >= 0 ) {
//            bytesRead = _in.read( _buffer, _currentBufferPosition,
//                                 (int) _chunkSize - _currentBufferPosition );
//            if ( bytesRead > 0 ) {
//                _currentBufferPosition += bytesRead;
//            } else if ( bytesRead == 0 ) {
//                throw new RuntimeException( "i'm doing something wrong" );
//            }
//        }
//        return bytesRead;
//    }

    /**
* Marks the data as fully written. This needs to be called before super.save()
*/
    private void _finishData() {
        if (!_savedChunks) {
            _md5 = Util.toHex( _messageDigester.digest() );
            _md5Pool.done( _messageDigester );
            _messageDigester = null;
            _length = _totalBytes;
            _savedChunks = true;
        }
    }
    
    public void close() throws IOException {
        //TODO:
    }

    //private final InputStream _in;
    //private StreamingDataPocketKnife _sdpk;
    private boolean _closeStreamOnPersist;
    private boolean _savedChunks = false;
    private ByteBuffer _buffer = null;
    private int _currentChunkNumber = 0;
    private int _currentBufferPosition = 0;
    private long _totalBytes = 0;
    private MessageDigest _messageDigester = null;
    //private OutputStream _outputStream = null;

    /**
* A pool of {@link java.security.MessageDigest} objects.
*/
    static SimplePool<MessageDigest> _md5Pool
            = new SimplePool<MessageDigest>( "md5" , 10 , -1 , false , false ) {
        /**
* {@inheritDoc}
*
* @see com.mongodb.util.SimplePool#createNew()
*/
        protected MessageDigest createNew() {
            try {
                return MessageDigest.getInstance( "MD5" );
            } catch ( java.security.NoSuchAlgorithmException e ) {
                throw new RuntimeException( "your system doesn't have md5!" );
            }
        }
    };
    
    public void printBuffer(ByteBuffer bb){
        System.out.println("printBuffer:");
        bb.flip(); //read from the buffer
//        System.out.println("capacity: " + bb.capacity());
//        System.out.println("limit: " + bb.limit());
//        System.out.println("position: " + bb.position());       
        for(int i=0;i<bb.limit();i++){
            //System.out.println("[:]");
            System.out.print((char)bb.get(i));
        }
        bb.compact();
//        System.out.println("capacity: " + bb.capacity());
//        System.out.println("limit: " + bb.limit());
//        System.out.println("position: " + bb.position());
    }

    /**

*/

}