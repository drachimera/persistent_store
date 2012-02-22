/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest;

import java.util.logging.Level;
import java.util.logging.Logger;
import us.kbase.psrest.resources.*;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import org.glassfish.grizzly.http.server.HttpServer;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import us.kbase.psrest.util.SystemProperties;


/**
 *
 * @author Daniel J. Quest
 * @date   January 9 2012
 */
public class WebServer {  
  
      private static URI getBaseURI() {
        try {
            SystemProperties sysprop = new SystemProperties();
            Integer port = new Integer(sysprop.get("psrest_port"));
            return UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return UriBuilder.fromUri("http://0.0.0.0/").port(7037).build();
      }
  
      public static final URI BASE_URI = getBaseURI();
  
      protected static HttpServer startServer() throws IOException {
          System.out.println("Starting grizzly...");
          ResourceConfig rc = new PackagesResourceConfig("us.kbase.psrest.resources");
          return GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
      }
      
      public static void main(String[] args) throws IOException {
          HttpServer httpServer = startServer();
          System.out.println(String.format("Jersey app started with WADL available at "
                  + "%sapplication.wadl\nTry out %sps/status\nHit control-C to stop it...",
                  BASE_URI, BASE_URI));
          //String inchar = "d";
          //Scanner scan = new Scanner (System.in);
          //while( !inchar.equalsIgnoreCase("q") ){
          //    inchar = scan.next();
          int foo = 5;
          while (foo > 2 ){
              foo = 6;
            try {
                Thread.sleep(9999999000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
          httpServer.stop();
      }    
    
}
