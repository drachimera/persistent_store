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
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
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
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import us.kbase.psrest.GridFS.PSGridFS;
import us.kbase.psrest.GridFS.PSGridFSInputFile;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.Tokens;

    /**
     * This handler using non-blocking streams to read POST data and store it
     * to the local file.
     */
    public class NonBlockingUploadHandler extends HttpHandler {
        
        private static String fileroot = ".";
        public NonBlockingUploadHandler(String stagingPath){
            fileroot = stagingPath;
        }
        
        private final AtomicInteger counter = new AtomicInteger();

        // -------------------------------------------- Methods from HttpHandler


        /**
         * When we start the non-blocking uploader, we need another thread in
         * the background to consume the output from the upload and load it into
         * mongo as it comes in.
         */
        
        @Override
        public void service(final Request request,
                            final Response response) throws Exception {
            
            // get file path
            final String path = request.getDecodedRequestURI();
            System.out.println(path);
            
            final String workspaceID = path.replaceAll("/ps/upload/", "");
            System.out.println("workspace: " + workspaceID);
            
            final String header = request.getHeader(Header.Server);
            System.out.println("header: " + header);
            
            System.out.println("attributes: ");
            Set<String> attributeNames = request.getAttributeNames();
            Iterator iter = attributeNames.iterator();
            while(iter.hasNext()){
                System.out.println(iter.next());
            }

            final NIOInputStream in = request.getInputStream(false);//.getNIOInputStream(); // put the stream in non-blocking mode
            
            final String filename = workspaceID + "_" + counter.incrementAndGet();
            //final FileChannel fileChannel = new FileOutputStream(fileroot + "/" + workspaceID + "/" + filename).getChannel();
            final FileChannel fileChannel = new FileOutputStream(fileroot + "/" + filename).getChannel();
            
            response.suspend();  // !!! suspend the Request

            // If we don't have more data to read - onAllDataRead() will be called
            in.notifyAvailable(new ReadHandler() {

                @Override
                public void onDataAvailable() throws Exception {
                    //LOGGER.log(Level.FINE, "[onDataAvailable] length: {0}", in.readyData());
                    storeAvailableData(in, fileChannel, workspaceID);
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
                    storeAvailableData(in, fileChannel, workspaceID);
                    response.setStatus(HttpStatus.ACCEPTED_202);
                    complete(false);
                    response.resume();
                }
                
                private void complete(final boolean isError) {
                    try {
                        fileChannel.close();
                        GridFSWright(filename, workspaceID);
                        //make sure to remove the file!
                        
                        //reset the metadata hashmap
                        httpmetadata = new HashMap();
                        
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
        private static void GridFSWright(String filename, String workspaceID){
            try {
                System.out.println("GridFS Saving: " + filename);
                InputStream instream = new FileInputStream(new File(fileroot + "/" + filename));
        //        System.out.println(instream.available());
                GridFS myFS = new GridFS(db, workspaceID);              // returns a default GridFS (e.g. "fs" root collection)
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
        private static PSGridFSInputFile GridFSCreateFile(InputStream instream, String filename, String workspaceID){
            System.out.println("GridFS Creating File: " + filename);
            PSGridFS gridfile = new PSGridFS(db, workspaceID);
            PSGridFSInputFile createFile = gridfile.createFile(instream, filename);
            return createFile;
        }
        
        private static void GridFSStoreChunk(){
            chunksStored++;
        }
        
        private static DBObject GridFSFinish(PSGridFSInputFile createFile){
            createFile.save();
            chunksStored = 0;
            DBObject metaData = createFile.getMetaData();
            return metaData;
        }
        
        private static ByteBuffer chunkbuf = ByteBuffer.allocate(Tokens.CHUNK_SIZE);
        private static HashMap httpmetadata = new HashMap();
        private static int chunksStored = 0;
        private static void storeAvailableData(NIOInputStream in, FileChannel fileChannel, String workspaceID)
                throws IOException {
            // Get the Buffer directly from NIOInputStream
            final Buffer buffer = in.readBuffer();
            // Retrieve ByteBuffer
            final ByteBuffer byteBuffer = buffer.toByteBuffer();
            //System.out.println("byteBuffer capacity: " +byteBuffer.capacity());
            StringBuilder foo = new StringBuilder();
            for(int i=0;i<byteBuffer.remaining();i++){
                char c = (char)byteBuffer.get(i);
                //System.out.print(c);
                foo.append(c);
            }
            
            try {
                while(byteBuffer.hasRemaining()) {
                    if(chunksStored == 0){
                        httpmetadata = parseMetaData(foo.toString());
                        GridFSStoreChunk();
                    }
                    // Write the ByteBuffer content to the file
                    fileChannel.write(byteBuffer);
                    System.out.println("chunksStored: " + chunksStored);
                }
            } finally {
                // we can try to dispose the buffer
                buffer.tryDispose();
            }
        }
        
        private final static String bodystart = "bodystart";
        private final static String dashkey = "dashkey"; //represents something like: ------------------------------522aea1eedd1
        private final static String dashes = "------------------------------";
        private static HashMap parseMetaData(String bufferContents){
              //System.out.println("parseMetaData...");
              //System.out.println("bufferContents...");
              //System.out.println(bufferContents);
              String[] split = bufferContents.split("\r\n\r\n");
//              for(int i=0;i<split.length;i++){
//                  System.out.println("\t" + split[i]);
//              }
              if(split.length > 0){ httpmetadata = parseHeader(httpmetadata, split[0]); }
              if(split.length > 1){ httpmetadata.put(bodystart, removeFooter(split[1])); }
              printHash(httpmetadata);
              return httpmetadata;
        }
        
        private static HashMap parseHeader(HashMap meta, String header){
            //System.out.println("parseHeader");
            //System.out.println(header); 
            String removeCarRet = header.replaceAll("\r", "");
            String[] lines = removeCarRet.split("\n");
            for(int i=0;i<lines.length;i++){
                  //System.out.println("\t" + lines[i]);
                  if(lines[i].contains(dashes)){ meta.put(dashkey, lines[i].trim()); }
                  else if(lines[i].contains(";")){ meta = parseSemiColonLine(meta, lines[i]); }
                  else if(lines[i].contains(":")){ meta = parseKeyValue(meta, lines[i], ":"); }
            }
            return meta;
        }
        
        private static HashMap parseSemiColonLine(HashMap meta, String line){
            //System.out.println("parseSemiColonLine");
            String[] split = line.split(";");
            for(int i = 0; i< split.length; i++){
                if(split[i].contains(":")){ meta = parseKeyValue(meta, split[i], ":"); }
                if(split[i].contains("=")){ meta = parseKeyValue(meta, split[i], "="); }
            }
            return meta;
        }
        
        private static HashMap parseKeyValue(HashMap meta, String line, String delim){
            //System.out.println("parseKeyValue");
            String[] split = line.split(delim);
            if(split.length == 2){
                //System.out.println("split.length: 2");
                meta.put(split[0].trim(), split[1].trim());
            }
            return meta;
        }
        
        private static String removeFooter(String bodyChunk){
            //System.out.println("removeFooter");
            String regex = "\r\n" + dashes + ".*--\r\n";
            String ret = bodyChunk.replaceAll(regex, "");//fixme!
            //System.out.println( ret );
            return ret;
        }
        
        private static void printHash(HashMap meta){
            System.out.println("printHash: ");
            Set entrySet = meta.keySet();
            Iterator iterator = entrySet.iterator();
            while(iterator.hasNext()){
                String key = (String) iterator.next();
                String value = meta.get(key).toString();
                System.out.println("\t" + key + " : " + value.replaceAll("\n", "<newline>"));
            }
        }

    } // END NonBlockingUploadHandler           
