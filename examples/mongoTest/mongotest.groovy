import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.mongodb.util.JSON;
import java.util.List;
import org.bson.types.BSONTimestamp;

print "Libs Included\n";

//start functions:
public DB connect(Mongo m, String databasename){
    DB db = null;
    try {
        db = m.getDB( databasename ); 
        System.out.println("Connected to Mongo");
    } catch (UnknownHostException ex) {
        Logger.getLogger(MongoExample.class.getName()).log(Level.SEVERE, null, ex);
    } catch (MongoException ex) {
        Logger.getLogger(MongoExample.class.getName()).log(Level.SEVERE, null, ex);
    }
    	return db;
}

    public boolean authenticate(String user, String password){
        DB db = connect();
        boolean auth = db.authenticate(user, password.toCharArray());
        return auth;
    }
    
    public void listDatabases(Mongo m){
        System.out.println("list databases");
        // get db names
	itter = m.getDatabaseNames().iterator();
        while (itter.hasNext()) {
	    s = itter.next();
            System.out.println("- [" + s + "]");
        }
    }
    
    public void listCollections(DB db){
        System.out.println("list collections");
        Set<String> colls = db.getCollectionNames();
        for (String s : colls) {
            System.out.println(s);
        }
    }

    public DBCollection getCollection(String collectionName, DB db){
        System.out.println("get collection");
        DBCollection coll = db.getCollection(collectionName);
        return coll;
    }

    public void demo1(DBCollection coll){
        System.out.println("demo1");
//> j = { name : "mongo" };
//{"name" : "mongo"}
        BasicDBObject j = new BasicDBObject();
        j.put("name", "Mongo");
//> t = { x : 3 };
//{ "x" : 3  }
        BasicDBObject t = new BasicDBObject();
        t.put("x", 3);
//> db.things.save(j);
        coll.save(j); //note the difference between save and insert 
                      // http://groups.google.com/group/mongodb-user/browse_thread/thread/b16bdd6579e5c3c9?pli=1
        
//> db.things.save(t);
        coll.save(t); 
//> db.things.find();
//{ "_id" : ObjectId("4c2209f9f3924d31102bd84a"), "name" : "mongo" }
//{ "_id" : ObjectId("4c2209fef3924d31102bd84b"), "x" : 3 }
//>
        DBCursor curs = coll.find();
        while(curs.hasNext()){
            System.out.println(curs.next().toString());
            // same thing is: System.out.println(curs.next());
        }
        
        return;
    }
    
    public void demo2(DBCollection coll){
        System.out.println("demo2");
        
//> for (var i = 1; i <= 20; i++) db.things.save({x : 4, j : i});
        for(int i = 1; i<=20; i++){
            BasicDBObject bo = new BasicDBObject();
            bo.put("x", 4);
            bo.put("j", i);
            coll.save(bo);
        }
//> db.things.find();
//{ "_id" : ObjectId("4c2209f9f3924d31102bd84a"), "name" : "mongo" }
//{ "_id" : ObjectId("4c2209fef3924d31102bd84b"), "x" : 3 }
//{ "_id" : ObjectId("4c220a42f3924d31102bd856"), "x" : 4, "j" : 1 }
//{ "_id" : ObjectId("4c220a42f3924d31102bd857"), "x" : 4, "j" : 2 }
//{ "_id" : ObjectId("4c220a42f3924d31102bd858"), "x" : 4, "j" : 3 }
//{ "_id" : ObjectId("4c220a42f3924d31102bd859"), "x" : 4, "j" : 4 }
//...
//note in javascript this stalls at 18 (buffer size) 
//to see it all do: 
//> var cursor = db.things.find();
//> while (cursor.hasNext()) printjson(cursor.next());
//or
//> db.things.find().forEach(printjson);
//in java it does not matter, just do this:
        DBCursor curs = coll.find();
        while(curs.hasNext()){
            System.out.println(curs.next().toString());
        }
        
//In the mongo shell, you can also treat cursors like an array :
//> var cursor = db.things.find();
//> printjson(cursor[4]);
//{ "_id" : ObjectId("4c220a42f3924d31102bd858"), "x" : 4, "j" : 3 }
//The java solution is not nearly as cool :(
//??????

    }
    
    public void findObjectGivenJSON(String JSONString, DBCollection col){
        System.out.println("findObjectGivenJSON");
        BasicDBObject bo = (BasicDBObject) JSON.parse(JSONString); //JSON2BasicDBObject
        DBCursor dbc = col.find(bo);
        System.out.println(dbc.next().toString());
    }
    
    public long countObjectsInCollection(DBCollection col){
        System.out.println("total # of documents: " + col.getCount());
        return col.getCount();
    }
    
    public void query1(DBCollection col){
        System.out.println("query1");
        //select * from things where name = "Mongo";
        //> db.things.find({name:"Mongo"}).forEach(printjson);
        //{ "_id" : ObjectId("4c2209f9f3924d31102bd84a"), "name" : "mongo" }
        String JSONString = "{\"name\":\"Mongo\"}";
        BasicDBObject bo = (BasicDBObject) JSON.parse(JSONString); //JSON2BasicDBObject
        DBCursor dbc = col.find(bo);
        while(dbc.hasNext()){
            System.out.println(dbc.next().toString());
        }
    }
    
    public void query2(DBCollection col){
        System.out.println("query2");
        //SELECT j FROM things WHERE x=4
        //> db.things.find({x:4}, {j:true}).forEach(printjson);
        //show an alternative way to construct the same sort of query as query1 (faster)
        BasicDBObject bo = new BasicDBObject();
        bo.put("x", 4);
        bo.put("j", new BasicDBObject("$exists", true));
        DBCursor dbc = col.find(bo);
        while(dbc.hasNext()){
            System.out.println(dbc.next().toString());
        }       
    }
    
    public void query3(DBCollection col){
        System.out.println("query3");
        // range query with multiple contstraings
        BasicDBObject decending = new BasicDBObject();
        decending.put("j", -1); // use 1 for accending
        BasicDBObject query = new BasicDBObject();
        query.put("j", new BasicDBObject("$gt", 2).append("$lte", 9)); // i.e. 20 < i <= 30
        DBCursor cur = col.find(query).sort(decending);
        while(cur.hasNext()) {
            System.out.println(cur.next());
        }
    }
    
    public void createAndCountIndex(DBCollection coll){
        System.out.println("createAndCountIndex");
        // create an index on the "j" field
        BasicDBObject dbo = new BasicDBObject("j", 1);
        coll.createIndex(dbo); // create index on "j", ascending


        // list the indexes on the collection
        List<DBObject> list = coll.getIndexInfo();
        for (DBObject o : list) {
            System.out.println(o);
        }
        
        //now drop the index
        coll.dropIndex(dbo);
    }
 
    public void testErrors(DB db){
        System.out.println("testErrors");
            // See if the last operation had an error
        System.out.println("Last error : " + db.getLastError());

        // see if any previous operation had an error
        System.out.println("Previous error : " + db.getPreviousError());

        // force an error
        db.forceError();

        // See if the last operation had an error
        System.out.println("Last error : " + db.getLastError());

        db.resetError();
    }

    //Trick to get the id of a newly inserted object...    
    //BasicDBObject obj = new BasicDBObject();
    //coll.save(obj);
    //ObjectId id = coll.get("_id"); 
    
    
    public void deleteCollection(DBCollection col){
        col.drop();
    }
    //drop a database
    //m.dropDatabase("com_mongodb_MongoAdmin");



//String host = "10.0.8.41";//"140.221.92.71";//"192.168.6.188"; //"localhost";
String host = "140.221.92.69";
int port = 27017;

if(args.length != 2){
	print "Usage: mongotest <HOST e.g. 140.221.92.69> <PORT e.g. 27017>";
	System.exit(0);
}
else { 
host = args[0];
port = new Integer(args[1]);
}
String databasename = "test";//"workspace";//

Mongo m = new Mongo( host , port );
DB db = connect(m, databasename);
listDatabases(m);
//listCollections(db);
        //DBCollection col = mt.getCollection("things", db);
        //mt.demo1(col);
        //mt.demo2(col);
        //mt.countObjectsInCollection(col);
        //String json = "{ \"x\" : 4, \"j\" : 3 }";
        //mt.findObjectGivenJSON(json, col);
        //mt.query1(col);
        //mt.query2(col);
        //mt.query3(col);
       //mt.createAndCountIndex(col);
       //mt.testErrors(db);
//mt.deleteCollection(col);
