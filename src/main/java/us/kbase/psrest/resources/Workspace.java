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
import com.mongodb.util.JSON;
import java.net.UnknownHostException;
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
 * @author qvh
 */
@Path("/ps")
public class Workspace {
    
    Mongo m = MongoConnection.getMongo();
    DB db = m.getDB( Tokens.WORKSPACE_DATABASE );

    
     @GET
     @Path("/workspace/{workspaceid}")
     @Produces("text/xml")
     public String getWorkspace(@PathParam("workspaceid") String workspaceID) {
         System.err.println(workspaceID);
         return workspaceID;
     }
     
     @GET
     @Path("/documents/{workspaceid}")
     @Produces("application/json")
     public JSONObject getDocIDs(@PathParam("workspaceid") String workspaceID) {
         DBCollection coll = db.getCollection(workspaceID);
         DBCursor documents = coll.find();
         JSONObject docList = new JSONObject();
         int counter = 0;
         try {
             while(documents.hasNext()){
                    DBObject next = documents.next();
                    System.out.println(next.get("_id"));
                    docList.put(new Integer(counter).toString(), next.get("_id"));
                    counter++;
             }
         } catch (JSONException ex) {
            Logger.getLogger(Workspace.class.getName()).log(Level.SEVERE, null, ex);
         }
         System.err.println(workspaceID);
         return docList;
     }
     
     //ps/store/<key> 
     @PUT
     @Path("/store/{workspaceid}")
     @Produces("application/json")
     @Consumes("application/json")
     public JSONObject storeDocument( String message, @PathParam("workspaceid") String workspaceID){
         DBCollection coll = db.getCollection(workspaceID);
         BasicDBObject bo = (BasicDBObject) JSON.parse(message);
         WriteResult save = coll.save(bo);
         JSONObject response = new JSONObject();
         try {
             System.out.println(workspaceID);
             System.out.println(message);
             response.put("status", "success");
             response.put("mongoresponse", save.toString());
             response.put("content", message);
         } catch (JSONException ex) {
            Logger.getLogger(Workspace.class.getName()).log(Level.SEVERE, null, ex);
         }
         return response;
     }
     
//     @GET
//     @Produces("text/xml")
//     [ID1,ID2,…] – IDs are all Strings 	ps/documents/<key> 	Get the id for all documents in your workspace identified by <key>
     
     public static void main(String[] args){
         Workspace ws = new Workspace();
         String wstr = ws.getWorkspace("abc123");
         System.out.println(wstr);
     }
    
}

