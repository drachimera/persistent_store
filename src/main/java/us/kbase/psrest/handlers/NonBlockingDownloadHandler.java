/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.handlers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import java.io.File;
import java.io.FileInputStream;
import org.glassfish.grizzly.http.server.io.NIOOutputStream;
import org.glassfish.grizzly.http.server.io.WriteHandler;
import org.glassfish.grizzly.http.server.util.MimeType;
import org.glassfish.grizzly.memory.MemoryManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import us.kbase.psrest.GridFS.PSGridFS;
import us.kbase.psrest.GridFS.PSGridFSDBFile;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.SystemProperties;
import us.kbase.psrest.util.Tokens;

/**
 *
 * @author qvh
 * This handler using non-blocking streams to write large amount of data to
 * a client.
 */
public class NonBlockingDownloadHandler extends HttpHandler {
        private static final Logger LOGGER = Grizzly.logger(NonBlockingDownloadHandler.class);
        private static final int CHUNK_SIZE = Tokens.CHUNK_SIZE;
        private static final Mongo m = MongoConnection.getMongo();
        private static final DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
        
        private final File parentFolder;

        public NonBlockingDownloadHandler(final File parentFolder) {
            this.parentFolder = parentFolder;
        }
        
        // -------------------------------------------- Methods from HttpHandler
        public PSGridFSDBFile getFileFromMongo(String filename, String workspaceID){
//        try {
            System.out.println("gridFSdownload");
            System.out.println("filename: " + filename);
            PSGridFS myFS = new PSGridFS(db, workspaceID);             // returns a GridFS where  "contracts" is root
            PSGridFSDBFile findOne = myFS.findOne(filename);
            System.out.println("file length: " + findOne.getLength());
//            findOne.writeTo("/tmp/"+filename+"z");
            return findOne;
//        } catch (IOException ex) {
//            Logger.getLogger(NonBlockingDownloadHandler.class.getName()).log(Level.SEVERE, null, ex);
//        }
//            return null;
        }

       
        

        @Override
        public void service(final Request request,
                            final Response response) throws Exception {
            final SystemProperties sysprop = new SystemProperties();
            
            //System.out.println("Entering NonBlockingDownloadHandler...");
            
            // Disable internal Response buffering
            response.setBufferSize(0);
            
            // put the stream in non-blocking mode
            
            final NIOOutputStream output = response.getOutputStream(true);//false is suposed to be faster but creates strange errors!   //.getNIOOutputStream();
            
            // get file path
            final String url = request.getDecodedRequestURI();
            final String path = sysprop.get("scratch_path");
            String tmp = url.replaceAll("/ps/download/", "");
            final String workspaceID = tmp.replaceAll("/f/.*", "");//"w919196189ed3f63b5d98ac537c29bc1c75255a47";
            final String filename = tmp.replaceAll(".+/f/", "");//"upload1";
            
            System.out.println("path: " + path);
            System.out.println("parentFolder: " + parentFolder);
            System.out.println("workspace: " + workspaceID);
            //getFileFromMongo("upload1", workspaceID);
            //final GridFSDBFile gridFSfile = getFileFromMongo(filename, workspaceID);
            final PSGridFSDBFile gridFSfile = getFileFromMongo(filename, workspaceID);
            InputStream mongostream = null;
            //final File file = new File(parentFolder, path); -- use this to get a local file
            
            
            // check if file exists
//            if (!file.isFile()) {
//                response.setStatus(HttpStatus.NOT_FOUND_404);
//                return;
//            }
            if (gridFSfile == null) {
                //System.out.println("ISSUE HTTPSTATUS.NOT_FOUND_404!");
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return;
            }else {
               mongostream  = gridFSfile.getInputStream();
            }
            
            //use this if you want to get a local file
            //final FileChannel fileChannel =
            //        new FileInputStream(file).getChannel();
            final ReadableByteChannel in = Channels.newChannel(mongostream);
            
            // set content-type
            final String contentType = MimeType.getByFilename(path);
            response.setContentType(contentType != null ? contentType : "binary/octet-stream");
            
            response.suspend();  // !!! suspend the Request

            // Notify the handler once we can write CHUNK_SIZE of data
            output.notifyCanWrite(new WriteHandler() {
                
                // keep the remaining size
                //private volatile long size = file.length();
                private volatile long size = gridFSfile.getLength();
                private volatile long chunksent = 0;
                private final long totalchunks = gridFSfile.numChunks();
                
                @Override
                public void onWritePossible() throws Exception {
                    LOGGER.log(Level.FINE, "[onWritePossible]");
                    //System.out.println("onWritePossible");
                    // send CHUNK of data
                    final boolean isWriteMore = sendChunk();

                    if (isWriteMore) {
                        // if there are more bytes to be sent - reregister this WriteHandler
                        output.notifyCanWrite(this, Tokens.CHUNK_SIZE);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    //System.out.println("onError");
                    LOGGER.log(Level.WARNING, "[onError] ", t);
                    response.setStatus(500, t.getMessage());
                    complete(true);
                }

                
                /**
                 * Send next CHUNK_SIZE of file - this version does not work on large files!
                 */
                private boolean sendChunk() throws IOException {
                    //System.out.println("Send Chunk...");
                    final MemoryManager mm = request.getContext().getMemoryManager();
                    final Buffer buffer = mm.allocate(Tokens.CHUNK_SIZE);
                    // mark it available for disposal after content is written
                    //buffer.allowBufferDispose(true);
                    final byte[] justReadBytes = gridFSfile.getChunk( (int) chunksent);
                    //System.out.println("chunks sent: " + chunksent);
                    //for(int j = 0; j<400; j++){
                    //    System.out.print((char) justReadBytes[j]);
                    //}
                    //System.out.println("\n");
                    //prepare buffer for reading
                    //buffer.position(justReadBytes.length);
                    //buffer.trim();
//                    if (justReadBytes.length <= 0) {
//                        complete(false);
//                        return false;
//                    }
                    // write the Buffer
                    //buffer.put(justReadBytes);
                    //output.write(buffer);
                    output.write(justReadBytes);
                    chunksent++;

                    
                    // check the remaining size here to avoid extra onWritePossible() invocation
                    if (totalchunks <= chunksent) {
                        complete(false);
                        return false;
                    }
                    
                    return true;
                }
                
                /**
                 * Send next CHUNK_SIZE of file - this version does not work on large files!
                 * 
                 * This one is not used!
                 */
                private boolean sendChunkSmall() throws IOException {
                    // allocate Buffer
                    //System.out.println("Send Chunk Small...");
                    final MemoryManager mm = request.getContext().getMemoryManager();
                    final Buffer buffer = mm.allocate(CHUNK_SIZE);
                    // mark it available for disposal after content is written
                    buffer.allowBufferDispose(true);
                                        
                    // read file to the Buffer
                    //final int justReadBytes = fileChannel.read(buffer.toByteBuffer());
                    final int justReadBytes = in.read(buffer.toByteBuffer());
                    if (justReadBytes <= 0) {
                        complete(false);
                        return false;
                    }
                    
                    // prepare buffer to be written
                    buffer.position(justReadBytes);
                    buffer.trim();
                    
                    // write the Buffer
                    output.write(buffer);
                    size -= justReadBytes;
                    
                    // check the remaining size here to avoid extra onWritePossible() invocation
                    if (size <= 0) {
                        complete(false);
                        return false;
                    }
                    
                    return true;
                }

                /**
                 * Complete the download
                 */
                private void complete(final boolean isError) {
                    //System.out.println("complete, isError: " + isError);
                    try {
                        //System.out.println("fileChannel.close()");
                        //fileChannel.close();
                        in.close();
                    } catch (IOException e) {
                        if (!isError) {
                            //System.out.println("!isError");
                            response.setStatus(500, e.getMessage());
                        }
                    }
                    
                    try {
                        //System.out.println("output.close()");
                        output.close();
                    } catch (Exception e) {
                        if (!isError) {
                            response.setStatus(500, e.getMessage());
                        }
                    }
                    //System.out.println("response.isSuspended()");
                    if (response.isSuspended()) {
                        //System.out.println("response.resume()");
                        response.resume();
                    } else {
                        //System.out.println("response.finish()");
                        response.finish();                    
                    }                    
                }
            }, Tokens.CHUNK_SIZE);
        }
    } // END NonBlockingDownloadHandler    
