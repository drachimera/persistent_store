/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;

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
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import us.kbase.psrest.util.CreateUser;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.SystemProperties;
import us.kbase.psrest.util.Tokens;

/**
 *
 * @author Daniel Quest
 * This class helps users manage workspaces... and provides ease of use 
 * 
 * { 
 * "userid" : "danquest",
 * "oauth"  : "abc123"
 * "workspaces" : {
 *      "w21dd8db6d0c13d09aa4151bd4dfd8c832d57c1a3" : {"alias" : "workspace1", "perms" : "R"},
 *      "w360df574022097b920202483bcf099261dc1f7"   : {"alias" : "workspace2", "perms" : "W"},
 *      "w360df574022097b920202483bcf099261dc1f7ab" : {"alias" : "silly_workspace", "perms" : "W"}
 *  }
 * }
 * 
 */
@Path("/ps/ws")
public class User {
    
    Mongo m = MongoConnection.getMongo();
    
    //create workspace by alias
    @Path("/{ownerid}/a/{alias}")
    public String provisionByAlias(@PathParam("ownerid") String ownerID, @PathParam("alias") String alias ) {
        Provision p = new Provision();
        String provision = p.provision(ownerID);
        System.out.println(provision);
        return provision;
    }
    //list workspaces owned by a user
    //list users
    //list alias names
    //given workspace_id give back alias
    //change alias
    //provide access to a workspace by alias
    
    public void create_user(String username){
        CreateUser cu = new CreateUser();
        cu.create_user(username);
    }
    
}
