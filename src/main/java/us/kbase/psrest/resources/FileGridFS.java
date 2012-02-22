/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;


import com.mongodb.WriteResult;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;
import java.io.File;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.SystemProperties;
import us.kbase.psrest.util.Tokens;

/**
 *
 * @author Daniel Quest
 * This class uses gridFS to store raw files as binary to mongoDB
 */
@Path("/ps/file")
public class FileGridFS {
      Mongo m = MongoConnection.getMongo();
    
     /**
      * save a file with name <filename> to a workspace, <workpace_id> 
      * filename is just some human readable name for the file, it is useful
      * later in 
      * @param workspaceID
      * @return 
      */
     @PUT
     @Path("/{workspace_id}/n/{filename}")
     @Consumes("text/plain")
     @Produces("application/json")
     public String saveFile(@PathParam("workspace_id") String workspaceID, @PathParam("filename") String filename, InputStream instream) {
        DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
        System.out.println("********************************");
        System.out.println(instream.toString());
        System.out.println("doing instream read");
        System.out.println("********************************");
        try {
            System.out.println(instream.read());
        } catch (IOException ex) {
            Logger.getLogger(FileGridFS.class.getName()).log(Level.SEVERE, null, ex);
        }
//        System.out.println(instream.available());
        GridFS myFS = new GridFS(db, workspaceID);              // returns a default GridFS (e.g. "fs" root collection)
        GridFSInputFile createFile = myFS.createFile(instream, filename);
        //createFile.setFilename(filename);
        createFile.save();
        DBObject metaData = createFile.getMetaData();
        return metaData.toString();
     }
     
     @GET
     @Path("/{workspace_id}")
     @Produces("application/json")
     public String listFiles(@PathParam("workspace_id") String workspaceID){
         String results = "{\n";
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         GridFS myFS = new GridFS(db, workspaceID);
         DBCursor fileList = myFS.getFileList();
         while(fileList.hasNext()){
            DBObject next = fileList.next();
            results += "\"" + next.get("filename") +"\" : ";  //if people call 2 things by the same name, this will not show duplicates
            results += next.toString();
         }
         results += "}\n";
         return results;
     }
     
    
}
