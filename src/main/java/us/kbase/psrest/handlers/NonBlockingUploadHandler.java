/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.handlers;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
            
            final FileChannel fileChannel = new FileOutputStream( //FileInputStream ( 
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
                    System.out.println("Counter: " + counter);
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

        private static final int buffersize = 1024;
        private static HashMap httpkv = new HashMap();
        private static final String httpdashes = "------------------------------";
        private static StringBuffer history = new StringBuffer ("");
        private static char delim = '-';
        private static boolean headerParsed = false;
        private static void storeAvailableData(NIOInputStream in, FileChannel fileChannel)
                throws IOException {
            char c = '.'; char p; //current char, c and previous p
            // Get the Buffer directly from NIOInputStream
            final Buffer buffer = in.readBuffer();
            // Retrieve ByteBuffer
            final ByteBuffer byteBuffer = buffer.toByteBuffer();
            ByteBuffer alloc = byteBuffer.allocate(buffersize);
            
            try {
                while(byteBuffer.hasRemaining()) {
                       //System.out.println(history.length());
                        p = c;
                        c = (char) byteBuffer.get();
                        //System.out.print( c );
                        history.append(c);
                        if(history.length() > buffersize){
                            //chop off the first char
                            history.deleteCharAt(0);
                        }
                        //*************************
                        // This is the logic to remove all http specific headers and footers as we process the
                        // input stream into chunks that will be stored.
                        //*************************
                        //System.out.println(" : " + history );
                        //if the history contains the start of the httpheader... record it for later
                        if(history.length() == buffersize-2 || c == '-' && p == '-'){//this is just to make it fast
                            String currhist = history.toString();
                            //this would be the first time we see the httpheader (calling it dashline)
                            if((currhist.startsWith(httpdashes) && currhist.length() > httpdashes.length()+12) && !httpkv.containsKey("dashline")){
                                //System.out.println(c + " : " + history );
                                //System.out.println("************************");
                                httpkv.put("dashline", currhist.substring(0, httpdashes.length()+12));
                                //determine the delimiter used in the header to seperate fields
                                delim = currhist.charAt(httpdashes.length()+12);
                                //System.out.println("#delim:" + delim + "_");                                
                            }
                            //if the header was parsed, then start checking for the footer, we need to remove that
                            String footer = httpkv.get("dashline")+"--";
                            if(currhist.contains(footer)){
                                currhist.replace(currhist, "");
                            }
                            //parse the http header
                            if(currhist.length() > 188 && headerParsed == false){ // make sure we have enough history to have a complete header
                                parseHeader(currhist, httpkv);
                                headerParsed = true;
                                printHash(httpkv);
                                //System.out.print(httpkv.get("body-when-header-cut"));
                                writeData((String)httpkv.get("body-when-header-cut"), fileChannel);
                            }
                        }else {
                            //System.out.print(c);
                        }                   
                    // Write the ByteBuffer content to the file
                    //legacy -- if we wanted to write to a file we could do this
                    //fileChannel.write(byteBuffer);  
                }//end while loop
                //save the final chunk... but make sure to remove the http 'footer'
                
                
            } finally {
                // we can try to dispose the buffer
                buffer.tryDispose();
                history = new StringBuffer ("");
                httpkv.clear();
                headerParsed = false;
            }
        }
        
        private static void writeData(String data, FileChannel channel) throws IOException{
            ByteBuffer buf = ByteBuffer.allocate(buffersize);
            buf.clear();
            buf.put(data.getBytes());
            buf.flip();
            while(buf.hasRemaining()) {
                channel.write(buf);
            }
            return;
            
        }
        
        private static HashMap parseHeader(String history, HashMap headerkv){
            //first take out the header from the body.
            String[] cut = history.split("" + delim + "\n" + delim);
            String header = cut[0]; //cut[1] is part of the body...
            if(cut.length > 1) headerkv.put("body-when-header-cut", cut[1]);
            String[] split = header.split("" + delim);
            for(int i=0;i<split.length;i++){
                   System.out.println(split[i]);
                   if(split[i].contains(";")){
                       headerkv = parseSemiColonLine(split[i], headerkv);
                   }
                   else if(split[i].contains(":")){
                       headerkv = parseKeyValue(split[i], headerkv);
                   }
            }
            return headerkv;
        }
        
        private static HashMap parseSemiColonLine(String line, HashMap hm){
            String[] split = line.split(";");
            for(int i=0;i<split.length;i++){
                String replaceAll = split[i].replaceAll(";", "");
                System.out.println(replaceAll);
                hm = parseKeyValue(replaceAll, hm);
            }
            return hm;
        }
        
        private static HashMap parseKeyValue(String line, HashMap hm){
            int indexOf = line.indexOf("="); //get the first regardless
            if(indexOf > line.length() || indexOf < 0) indexOf = line.indexOf(":");
            if(indexOf > line.length() || indexOf < 0) return hm;
            String key = line.substring(0, indexOf).trim();
            String value = line.substring(indexOf+1).trim();
            hm.put(key, value);
            return hm;
        }
        
        private static void printHash(HashMap hm){
            System.out.println("<hashmap>");
            Set<String> keySet = hm.keySet();
            for(String key : keySet){
                System.out.println(key + " : " + (String)hm.get(key));
            }
            System.out.println("</hashmap>");
        }
        
        private static void GridFSWright(DB db, String workspaceID, String filename, FileChannel fileChannel){
            
            GridFS myFS = new GridFS(db, workspaceID);              // returns a default GridFS (e.g. "fs" root collection)
            GridFSInputFile createFile = myFS.createFile(filename);
            
        }

} // END NonBlockingUploadHandler          
