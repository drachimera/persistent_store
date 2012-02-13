
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is an extension of the java Properties class. The additional
 * functionality mainly deals with finding the properties file. The search
 * order for the files are:
 *   0. uses the value set by the static setFile() method.
 *   1. uses the file defined by the user's SYS_PROP environment variable.
 *   2. uses the file named sys.properties in the user's home directory.
 *   3. uses the file defined by the java System property SYS_PROP.
 *   4. uses /etc/sys.properties.
 * @author Thomas S. Brettin
 * @author Daniel J. Quest
 */
public class SystemProperties {

    private static final String SYS_PROP = "SYS_PROP";
    private static String file = null;
    private Properties prop = null;

    /**
     * We are going to run this code once, the first time the class is
     * referenced.
     */
    static {

        if (System.getenv(SYS_PROP) != null) {
            file = System.getenv(SYS_PROP);
        } else if (new File(System.getenv("HOME") + "/sys.properties").exists() == true) {
            file = System.getenv("HOME") + "/sys.properties";
        } else if (System.getProperty(SYS_PROP) != null) {
            file = System.getProperty(SYS_PROP);
        } else {
            //file = "/etc/sys.properties";
            file = System.getProperty("user.dir") + "/conf/sys.properties";
        }

        Logger.getLogger(SystemProperties.class.getName()).log(Level.INFO,
                "using " + file + " for sys.properties");
    }

    /**
     * Allows the name of the file to be changed at any time. However, the change
     * won't take effect until the constructor is called. Once the constructor
     * is called, the properties change to reflect the new file. All subsequent
     * calls to the constructor will use the new file. All previous instances
     * will still hold the old properties, but the new file name (UGH!).
     * @param f The path and name of the properties file.
     */
    public static void setFile(String f) {
        file = f;
    }

    public SystemProperties() throws IOException {
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(file);
            prop = new Properties();
            prop.load(inStream);
        } catch (IOException ex) {
            Logger.getLogger(SystemProperties.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(SystemProperties.class.getName()).log(Level.SEVERE, null, ex);
                throw ex;
            }
        }
    }

    /**
     * Gets the value of the property. Returns null if the property (key) is
     * not found in the properties file.
     * @param key The name of the property.
     * @return value The value of the property.
     */
    public String get(String key) {
        return prop.getProperty(key);
    }
    
    public Set<String> propertySet(){
        return this.prop.stringPropertyNames();
    }
}
