/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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
import com.mongodb.WriteResult;
import java.net.UnknownHostException;
import java.util.Set;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import org.bson.types.ObjectId;
import us.kbase.psrest.util.HashUtil;
import us.kbase.psrest.util.MongoConnection;
import us.kbase.psrest.util.SystemProperties;
import us.kbase.psrest.util.Tokens;


/**
 *
 * @author Daniel J. Quest
 * 
 * This class Provisions new Workspaces
 * 
 * Workspaces have metadata that is not controlled by the user of the workspace
 * {
 * _id : {$oid : "ABC123..."},  #The id assigned to the metadata object by Mongo
 * owner : "me"                 #The user/group in kbase who owns the workspace
 * key : "A1X2..."        #To update/save/delete, you must supply the key
 * timestamp : "1293765885000"  #When the workspace was created
 * }
 * 
 */

// The Java class will be hosted at the URI path "/workspace"
@Path("/ps/provision/{ownerid}")
public class Provision {
    Mongo m = MongoConnection.getMongo();
    
    String output = "";

    // The Java method will process HTTP PUT requests
    @PUT
    // The Java method will produce content identified by the MIME Media
    // type "text/plain"
    @Produces("text/plain")
    public String provision(@PathParam("ownerid") String ownerID) {
        String key = "invalidkey";
        try {
            // Return some cliched textual content
            DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
            DBCollection coll = db.getCollection(Tokens.METADATA_COLLECTION);
            BasicDBObject dbo = new BasicDBObject(); 
            dbo.put("owner", ownerID);         
            WriteResult wr = coll.save(dbo);
            ObjectId id = dbo.getObjectId("_id");
            System.out.println("ID: " + id.toString());
            key = HashUtil.SHA1(HashUtil.randcat(id.toString()));
            System.out.println("ID: " + key);
            dbo.put("key", key);
            BasicDBObject query = new BasicDBObject(); query.put("_id", id);
            coll.update(query, dbo);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Provision.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Provision.class.getName()).log(Level.SEVERE, null, ex);
        }
        return key;
    }
    

}

/*
 * 
 *             String output = "";
            Mongo m = MongoConnection.getMongo();
            DB db = m.getDB( Tokens.WORKSPACE_DATABASE );            
            Set<String> colls = db.getCollectionNames();
            if(colls.size() < 1) return "[]";
            for (String s : colls) {
                System.out.println(s);
                output = s + "," +  output;
            }
            output = output.subSequence(0, output.length()-1).toString();
            output = "[" + output + "]";


            DBCollection coll = db.getCollection(userID);
            DBCursor find = coll.find();
        
        
        
        return output;
 */
    
