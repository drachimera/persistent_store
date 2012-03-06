/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.handlers;

import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.io.NIOInputStream;
import org.glassfish.grizzly.http.server.io.ReadHandler;
import org.glassfish.grizzly.http.util.HttpStatus;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.Tokens;

    /**
     * This handler using non-blocking streams to read POST data and store it
     * to the local file.
     */
    public class NonBlockingUploadHandler extends HttpHandler {
        
        private final AtomicInteger counter = new AtomicInteger();

        // -------------------------------------------- Methods from HttpHandler


        @Override
        public void service(final Request request,
                            final Response response) throws Exception {
            
            // get file path
            final String path = request.getDecodedRequestURI();
            System.out.println(path);

            final NIOInputStream in = request.getInputStream(false);//.getNIOInputStream(); // put the stream in non-blocking mode
            
            final String filename = "./" + counter.incrementAndGet() + ".upload";
            final FileChannel fileChannel = new FileOutputStream(
                    filename).getChannel();
            
            response.suspend();  // !!! suspend the Request

            // If we don't have more data to read - onAllDataRead() will be called
            in.notifyAvailable(new ReadHandler() {

                @Override
                public void onDataAvailable() throws Exception {
                    //LOGGER.log(Level.FINE, "[onDataAvailable] length: {0}", in.readyData());
                    storeAvailableData(in, fileChannel);
                    in.notifyAvailable(this);
                }

                @Override
                public void onError(Throwable t) {
                    //LOGGER.log(Level.WARNING, "[onError]", t);
                    response.setStatus(500, t.getMessage());
                    complete(true);
                    
                    if (response.isSuspended()) {
                        response.resume();
                    } else {
                        response.finish();                    
                    }
                }

                @Override
                public void onAllDataRead() throws Exception {
                    //LOGGER.log(Level.FINE, "[onAllDataRead] length: {0}", in.readyData());
                    storeAvailableData(in, fileChannel);
                    response.setStatus(HttpStatus.ACCEPTED_202);
                    complete(false);
                    response.resume();
                }
                
                private void complete(final boolean isError) {
                    try {
                        fileChannel.close();
                        GridFSWright(filename);
                    } catch (IOException e) {
                        if (!isError) {
                            response.setStatus(500, e.getMessage());
                        }
                    }
                    
                    try {
                        in.close();
                    } catch (IOException e) {
                        if (!isError) {
                            response.setStatus(500, e.getMessage());
                        }
                    }                                        
                }
            });

        }
        
        private static final Mongo m = MongoConnection.getMongo();
        private static final DB db = m.getDB( Tokens.WORKSPACE_DATABASE );       
        private static void GridFSWright(String filename){
            try {
                InputStream instream = new FileInputStream(new File(filename));
        //        System.out.println(instream.available());
                GridFS myFS = new GridFS(db, "foobar");              // returns a default GridFS (e.g. "fs" root collection)
                GridFSInputFile createFile = myFS.createFile(instream, filename); 
                //createFile.setFilename(filename);
                createFile.save();
                DBObject metaData = createFile.getMetaData();
            } catch (IOException ex) {
                Logger.getLogger(NonBlockingUploadHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        
//        private static void GridFSWright(DB db, String workspaceID, String filename, FileChannel fileChannel){
//            
//            GridFS myFS = new GridFS(db, workspaceID);              // returns a default GridFS (e.g. "fs" root collection)
//            GridFSInputFile createFile = myFS.createFile(filename);
//            
//        }


        private static void storeAvailableData(NIOInputStream in, FileChannel fileChannel)
                throws IOException {
            // Get the Buffer directly from NIOInputStream
            final Buffer buffer = in.readBuffer();
            // Retrieve ByteBuffer
            final ByteBuffer byteBuffer = buffer.toByteBuffer();
            
            try {
                while(byteBuffer.hasRemaining()) {
                    // Write the ByteBuffer content to the file
                    fileChannel.write(byteBuffer);
                }
            } finally {
                // we can try to dispose the buffer
                buffer.tryDispose();
            }
        }

    } // END NonBlockingUploadHandler           
