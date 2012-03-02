/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.GridFS;

/**
 *
 * @author qvh
 */

import java.io.File;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.Util;


/**
* a simple CLI for Gridfs
*/
public class CLI {
    
    /**
* Dumps usage info to stdout
*/
    private static void printUsage() {
        System.out.println("Usage : [--bucket bucketname] action");
        System.out.println(" where action is one of:");
        System.out.println(" list : lists all files in the store");
        System.out.println(" put filename : puts the file filename into the store");
        System.out.println(" get filename1 filename2 : gets filename1 from store and sends to filename2");
        System.out.println(" md5 filename : does an md5 hash on a file in the db (for testing)");
    }

    private static String host = "127.0.0.1";
    private static String db = "test";
    
    private static Mongo _mongo = null;
    private static Mongo getMongo()
        throws Exception {
        if ( _mongo == null )
            _mongo = new Mongo( host );
        return _mongo;
    }
    
    private static PSGridFS _gridfs;
    private static PSGridFS getGridFS()
        throws Exception {
        if ( _gridfs == null )
            _gridfs = new PSGridFS( getMongo().getDB( db ) );
        return _gridfs;
    }

    public static void main(String[] args) throws Exception {
        
        if ( args.length < 1 ){
            printUsage();
            return;
        }
        
        Mongo m = null;

        for ( int i=0; i<args.length; i++ ){
            String s = args[i];
            
            if ( s.equals( "--db" ) ){
                db = args[i+1];
                i++;
                continue;
            }

            if ( s.equals( "--host" ) ){
                host = args[i+1];
                i++;
                continue;
            }

            if ( s.equals( "help" ) ){
                printUsage();
                return;
            }
            
            if ( s.equals( "list" ) ){
                PSGridFS fs = getGridFS();
                
                System.out.printf("%-60s %-10s\n", "Filename", "Length");
                
                for ( DBObject o : fs.getFileList() ){
                    System.out.printf("%-60s %-10d\n", o.get("filename"), ((Number) o.get("length")).longValue());
                }
                return;
            }
            
            if ( s.equals( "get" ) ){
                PSGridFS fs = getGridFS();
                String fn = args[i+1];
                PSGridFSDBFile f = fs.findOne( fn );
                if ( f == null ){
                    System.err.println( "can't find file: " + fn );
                    return;
                }

                f.writeTo( f.getFilename() );
                return;
            }

            if ( s.equals( "put" ) ){
                PSGridFS fs = getGridFS();
                String fn = args[i+1];
                PSGridFSInputFile f = fs.createFile( new File( fn ) );
                f.save();
                f.validate();
                return;
            }
            

            if ( s.equals( "md5" ) ){
                PSGridFS fs = getGridFS();
                String fn = args[i+1];
                PSGridFSDBFile f = fs.findOne( fn );
                if ( f == null ){
                    System.err.println( "can't find file: " + fn );
                    return;
                }

                MessageDigest md5 = MessageDigest.getInstance("MD5");
                md5.reset();
                DigestInputStream is = new DigestInputStream( f.getInputStream() , md5 );
                int read = 0;
                while ( is.read() >= 0 ){
                    read++;
                    int r = is.read( new byte[17] );
                    if ( r < 0 )
                        break;
                    read += r;
                }
                byte[] digest = md5.digest();
                System.out.println( "length: " + read + " md5: " + Util.toHex( digest ) );
                return;
            }
            
            
            System.err.println( "unknown option: " + s );
            return;
        }
        
    }

}