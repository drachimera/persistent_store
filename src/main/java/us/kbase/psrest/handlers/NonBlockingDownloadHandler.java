/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.handlers;

import java.io.File;
import java.io.FileInputStream;
import org.glassfish.grizzly.http.server.io.NIOOutputStream;
import org.glassfish.grizzly.http.server.io.WriteHandler;
import org.glassfish.grizzly.http.server.util.MimeType;
import org.glassfish.grizzly.memory.MemoryManager;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
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
        
        private final File parentFolder;

        public NonBlockingDownloadHandler(final File parentFolder) {
            this.parentFolder = parentFolder;
        }
        
        // -------------------------------------------- Methods from HttpHandler


        @Override
        public void service(final Request request,
                            final Response response) throws Exception {
            
            System.out.println("Entering NonBlockingDownloadHandler...");
            
            // Disable internal Response buffering
            response.setBufferSize(0);
            
            // put the stream in non-blocking mode
            
            final NIOOutputStream output = response.getOutputStream(false);//.getNIOOutputStream();
            
            // get file path
            final String path = request.getDecodedRequestURI();
            
            final File file = new File(parentFolder, path);
            
            System.out.println("path: " + path);
            System.out.println("parentFolder: " + parentFolder);
            
            
            // check if file exists
            if (!file.isFile()) {
                response.setStatus(HttpStatus.NOT_FOUND_404);
                return;
            }
            
            final FileChannel fileChannel =
                    new FileInputStream(file).getChannel();
            
            // set content-type
            final String contentType = MimeType.getByFilename(path);
            response.setContentType(contentType != null ? contentType : "binary/octet-stream");
            
            response.suspend();  // !!! suspend the Request

            // Notify the handler once we can write CHUNK_SIZE of data
            output.notifyCanWrite(new WriteHandler() {
                
                // keep the remaining size
                private volatile long size = file.length();
                
                @Override
                public void onWritePossible() throws Exception {
                    LOGGER.log(Level.FINE, "[onWritePossible]");
                    System.out.println("onWritePossible");
                    // send CHUNK of data
                    final boolean isWriteMore = sendChunk();

                    if (isWriteMore) {
                        // if there are more bytes to be sent - reregister this WriteHandler
                        output.notifyCanWrite(this, CHUNK_SIZE);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("onError");
                    LOGGER.log(Level.WARNING, "[onError] ", t);
                    response.setStatus(500, t.getMessage());
                    complete(true);
                }

                /**
                 * Send next CHUNK_SIZE of file
                 */
                private boolean sendChunk() throws IOException {
                    // allocate Buffer
                    System.out.println("Send Chunk...");
                    final MemoryManager mm = request.getContext().getMemoryManager();
                    final Buffer buffer = mm.allocate(CHUNK_SIZE);
                    // mark it available for disposal after content is written
                    buffer.allowBufferDispose(true);
                                        
                    // read file to the Buffer
                    final int justReadBytes = fileChannel.read(buffer.toByteBuffer());
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
                    System.out.println("complete, isError: " + isError);
                    try {
                        System.out.println("fileChannel.close()");
                        fileChannel.close();
                    } catch (IOException e) {
                        if (!isError) {
                            System.out.println("!isError");
                            response.setStatus(500, e.getMessage());
                        }
                    }
                    
                    try {
                        System.out.println("output.close()");
                        output.close();
                    } catch (Exception e) {
                        if (!isError) {
                            response.setStatus(500, e.getMessage());
                        }
                    }
                    System.out.println("response.isSuspended()");
                    if (response.isSuspended()) {
                        System.out.println("response.resume()");
                        response.resume();
                    } else {
                        System.out.println("response.finish()");
                        response.finish();                    
                    }                    
                }
            }, CHUNK_SIZE);
        }
    } // END NonBlockingDownloadHandler    