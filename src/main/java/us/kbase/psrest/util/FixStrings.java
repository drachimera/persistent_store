/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.util;

/**
 *
 * @author Daniel Quest
 * 
 * MongoDB does not accept some strings, so we need a central place to make sure that
 * strings are fixed before going to the database and then reverted before comming back to the user
 */
public class FixStrings {
    public static final String dot = "<dot>";
    public static final String dollar = "<dollar>";
    public static final String tilda = "<tilda>";
    
    public static String usr2mongo(String s){
        return s.replaceAll("\\.", dot).replaceAll("\\$", dollar).replaceAll("~", tilda);
    }
    
    public static String mongo2usr(String s){
        return s.replaceAll(dot, ".").replaceAll(dollar, "$").replaceAll(tilda, "~");
    }
    
}
