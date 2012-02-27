/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.handlers;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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


/**
 *
 * @author Daniel J Quest
 * This handler using non-blocking streams to read POST data and store it into MongoDB.
 */
public class NonBlockingUploadHandler extends HttpHandler {
    
    private static final Logger LOGGER = Grizzly.logger(NonBlockingUploadHandler.class);
        
        private final AtomicInteger counter = new AtomicInteger();

        // -------------------------------------------- Methods from HttpHandler


        @Override
        public void service(final Request request,
                            final Response response) throws Exception {

            final NIOInputStream in = request.getInputStream(false);//.getNIOInputStream(); // put the stream in non-blocking mode
            
            final FileChannel fileChannel = new FileOutputStream(
                    "./" + "upload" + counter.incrementAndGet() ).getChannel();
            
            response.suspend();  // !!! suspend the Request

            // If we don't have more data to read - onAllDataRead() will be called
            in.notifyAvailable(new ReadHandler() {

                @Override
                public void onDataAvailable() throws Exception {
                    LOGGER.log(Level.FINE, "[onDataAvailable] length: {0}", in.readyData());
                    storeAvailableData(in, fileChannel);
                    in.notifyAvailable(this);
                }

                @Override
                public void onError(Throwable t) {
                    LOGGER.log(Level.WARNING, "[onError]", t);
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
                    LOGGER.log(Level.FINE, "[onAllDataRead] length: {0}", in.readyData());
                    storeAvailableData(in, fileChannel);
                    response.setStatus(HttpStatus.ACCEPTED_202);
                    complete(false);
                    response.resume();
                }
                
                private void complete(final boolean isError) {
                    try {
                        fileChannel.close();
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
