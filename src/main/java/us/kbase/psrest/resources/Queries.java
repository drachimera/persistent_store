/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;

import javax.ws.rs.Path;

import com.mongodb.WriteResult;
import java.io.IOException;
import java.util.Map;
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
import java.util.Iterator;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import us.kbase.psrest.util.FixStrings;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.SystemProperties;
import us.kbase.psrest.util.Tokens;

/**
 *
 * @author Daniel Quest
 * This set of methods allows one to query the workspace using native mongo functionality
 */

@Path("/ps/q")
public class Queries {
     Mongo m = MongoConnection.getMongo();
         
     @GET
     @Path("/owner/list_workspaces/{user_id}")
     @Produces("application/json")
     public String getWorkspaceJSON(@PathParam("user_id") String userID) {
         //System.out.println("getWorkspaceJSON: " + userID);
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(Tokens.METADATA_COLLECTION);                   
         BasicDBObject query = new BasicDBObject();
         query.append(Tokens.OWNER, userID);
         DBCursor workspaces = coll.find(query);
         BasicDBObject docList = new BasicDBObject();
         int counter = 0;         
         while(workspaces.hasNext()){
                DBObject next = workspaces.next();
                //System.out.println(next.toString());
                docList.append(new Integer(counter).toString(), next);//next.get("_id"));
                counter++;
         }
         //System.err.println(docList.toString());
         return docList.toString() + "\n";
     }
     
     
}
