/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.util;

/**
 *
 * @author Daniel J. Quest
 * 
 * The tokens class contains application level constants that we don't want
 * users to modify through sys.properties
 */
public class Tokens {
    public static final String WORKSPACE_DATABASE = "workspace";
    public static final String USER_DATABASE = "user";    
    public static final String USER_COLLECTION = "users";    
    public static final String METADATA_COLLECTION = "meta";
    public static final int CHUNK_SIZE = 8192;
    
}
