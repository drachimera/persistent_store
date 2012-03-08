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
 * @author qvh
 */
@Path("/ps")
public class Workspace {
    
     Mongo m = MongoConnection.getMongo();
     
     /**
      * given a workspaceID - return all metadata about that workspace.
      * @param workspaceID
      * @return 
      */
     @GET
     @Path("/workspace/{workspaceid}")
     @Produces("application/json")
     public String getWorkspaceJSON(@PathParam("workspaceid") String workspaceID) {
         BasicDBObject dbo = new BasicDBObject(); 
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(Tokens.METADATA_COLLECTION);
         BasicDBObject query = new BasicDBObject(); 
         BasicDBObject bo = (BasicDBObject) JSON.parse("{ key :\"" + workspaceID + "\" }"); //JSON2BasicDBObject
         DBCursor dbc = coll.find(bo); 
         return dbc.next().toString() + "\n"; //need to catch exceptions and so on...
     }
     
     /**
      * save a document to the workspace provided
      * @param workspaceID
      * @return 
      */     
     @PUT
     @Path("/document/{workspaceid}")
     //@Consumes("application/json")
     @Produces("application/json")
     public String saveDocument(@PathParam("workspaceid") String workspaceID, String jsonString) { //, String jsonString
         //System.out.println(jsonString);
         //System.out.println(workspaceID);
         //System.out.println(jsonString);
         jsonString = FixStrings.usr2mongo(jsonString);
         System.out.println(jsonString);
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(workspaceID);
         BasicDBObject bo = (BasicDBObject) JSON.parse(jsonString);
         WriteResult save = coll.save(bo);
        //System.out.println(workspaceID);
         return FixStrings.mongo2usr(bo.toString()) + "\n";
     }
     
     /**
      * save a document to the workspace provided
      * @param workspaceID
      * @return 
      */     
     @POST
     @Path("/document/{workspaceid}")
     //@Consumes("application/json")
     @Produces("application/json")
     public String saveJSONDocument(@PathParam("workspaceid") String workspaceID, String jsonString) { //, String jsonString
         //System.out.println(jsonString);
         //System.out.println(workspaceID);
         //System.out.println(jsonString);
         jsonString = FixStrings.usr2mongo(jsonString);
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(workspaceID);
         BasicDBObject bo = (BasicDBObject) JSON.parse(jsonString);
         WriteResult save = coll.save(bo);
        //System.out.println(workspaceID);
         return FixStrings.mongo2usr(bo.toString()) + "\n";
     }
     
     @DELETE
     @Path("/delete_workspace/{workspaceid}")
     @Produces("application/json")
     public String deleteWorkspace(@PathParam("workspaceid") String workspaceID){
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(workspaceID);
         coll.drop();
         BasicDBObject bo = new BasicDBObject();
         bo.append("status", "workspace= " + workspaceID + " deleted");
         return bo.toString()+"\n"; 
     }
   
     @DELETE 
     @Path("/delete/{workspaceid}/document/{documentid}")
     @Produces("application/json")
     public String deleteDocument(@PathParam("workspaceid") String workspaceID, @PathParam("documentid") String documentID){        
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(workspaceID);
         DBObject deleteme = findbyIDquery(coll, documentID);
         coll.remove(deleteme);
         BasicDBObject bo = new BasicDBObject();
         bo.append("status", "document= " + workspaceID + " deleted");
         //System.out.println(bo.toString());
         return bo.toString(); 
     }
     
     public DBObject findbyIDquery(DBCollection coll, String docid){
         //use something like {_id:ObjectId("4f4feac16970c538d322f61d")} inside of findOne()
        DBObject searchById = new BasicDBObject("_id", new ObjectId(docid));
        return coll.findOne(searchById);
     }
     
     /**
      * save a document to the workspace provided
      * @param workspaceID
      * @return 
      */     
     @GET
     @Path("/document/find/{workspaceid}")
     @Consumes("application/json")
     @Produces("application/json")
     public String findDocument(@PathParam("workspaceid") String workspaceID, String jsonString) { //, String jsonString
         StringBuffer ret = new StringBuffer();
         ret.append("{\n");
         //System.out.println(jsonString);
         //System.out.println(workspaceID);
         //System.out.println(jsonString);
         int counter = 0;
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         DBCollection coll = db.getCollection(workspaceID);
         BasicDBObject bo = (BasicDBObject) JSON.parse(FixStrings.usr2mongo(jsonString));
         DBCursor find = coll.find(bo);
         Iterator<DBObject> iter = find.iterator();
         while(iter.hasNext()){
             counter++;
             if(counter > 1) ret.append(",\n");
             DBObject next = iter.next();
             Map toMap = next.toMap();
             ret.append("\"" + toMap.get("_id").toString() + "\" : ");
             //remove the redundant id
             next.removeField("_id"); 
             //ret+= "\"kbid" + counter + "\" : "; 
             String rec = FixStrings.mongo2usr(next.toString());
             ret.append(rec);
         }
         ret.append("\n}\n");
        //System.out.println(workspaceID);
         return ret.toString();
     }
     
     @GET
     @Path("/documents/{workspaceid}")
     @Produces("application/json")
     public JSONObject getDocIDs(@PathParam("workspaceid") String workspaceID) {
         
         DB db = m.getDB( Tokens.WORKSPACE_DATABASE );
         
         DBCollection coll = db.getCollection(workspaceID);
         DBCursor documents = coll.find();
         JSONObject docList = new JSONObject();
         int counter = 0;
         try {
             while(documents.hasNext()){
                    DBObject next = documents.next();
                    //System.out.println(next.get("_id"));
                    docList.put(new Integer(counter).toString(), next.get("_id"));
                    counter++;
             }
         } catch (JSONException ex) {
            Logger.getLogger(Workspace.class.getName()).log(Level.SEVERE, null, ex);
         }
         //System.err.println(workspaceID);
         return docList;
     }
     
//     //ps/store/<key> 
//     @PUT
//     @Path("/store/{workspaceid}")
//     @Produces("application/json")
//     @Consumes("application/json")
//     public JSONObject storeDocument( String message, @PathParam("workspaceid") String workspaceID){
//         DBCollection coll = db.getCollection(workspaceID);
//         BasicDBObject bo = (BasicDBObject) JSON.parse(message);
//         WriteResult save = coll.save(bo);
//         JSONObject response = new JSONObject();
//         try {
//             System.out.println(workspaceID);
//             System.out.println(message);
//             response.put("status", "success");
//             response.put("mongoresponse", save.toString());
//             response.put("content", message);
//         } catch (JSONException ex) {
//            Logger.getLogger(Workspace.class.getName()).log(Level.SEVERE, null, ex);
//         }
//         return response;
//     }
    
}

