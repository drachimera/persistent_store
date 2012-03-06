/*
 * This file runs the service...
 */
package us.kbase.psrest;

import com.sun.jersey.api.container.ContainerFactory;
import org.glassfish.grizzly.http.server.HttpHandler;
import us.kbase.psrest.handlers.*;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javax.ws.rs.core.UriBuilder;
import java.lang.management.ManagementFactory;
import java.net.URI;
import us.kbase.psrest.util.SystemProperties;
import java.io.File;
import org.glassfish.grizzly.http.server.NetworkListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import us.kbase.psrest.util.Tokens;


/**
 *
 * @author Daniel J. Quest
 * @date   January 9 2012
 */
public class WebServer { 
      private static final int DEFAULT_PORT = 7037;
      private static final Logger LOGGER = Grizzly.logger(WebServer.class);
  
      private static URI getBaseURI(String portKey) {//e.g. "psrest_port"
        try {
            SystemProperties sysprop = new SystemProperties();
            Integer port = new Integer(sysprop.get(portKey));
            return UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return UriBuilder.fromUri("http://0.0.0.0/").port(DEFAULT_PORT).build();
      }
  
      public static final URI BASE_URI = getBaseURI("psrest_port");
      
      //To see  what is going on under the hood in Jersey:
      //http://java.net/projects/jersey/sources/svn/content/trunk/jersey/jersey-grizzly2/src/main/java/com/sun/jersey/api/container/grizzly2/GrizzlyServerFactory.java?rev=5664  
      protected static HttpServer startServer() throws IOException {
          System.out.println("Starting PSREST...");
          startDownloader();
          startUploader();
          ResourceConfig rc = new PackagesResourceConfig("us.kbase.psrest.resources"); 
          HttpServer server = GrizzlyServerFactory.createHttpServer(BASE_URI, rc);
          return server;
      }
      
      
      protected static HttpServer startDownloader(){          
        try {
            // create a basic server that listens on port 8080
            SystemProperties sysprop = new SystemProperties();
            String downloadPort = sysprop.get("dowload_port");
            System.out.println("Starting Downloader on Port: " + downloadPort);
            final NetworkListener networkListener = new NetworkListener("downloader",
                    "0.0.0.0", new Integer(downloadPort));
            
            // limit the max async write queue size per connection
            // usually we have to make the size big enough to accept data chunk + HTTP headers.
            networkListener.setMaxPendingBytes(Tokens.CHUNK_SIZE * 4);
            
            final HttpServer server = new HttpServer();
            server.addListener(networkListener);
            
            final ServerConfiguration config = server.getServerConfiguration();

            // Map the NonBlockingUploadHandler to "/" URL
            //config.addHttpHandler(new NonBlockingDownloadHandler(new File(".") ), "/download");
            
            config.addHttpHandler(new NonBlockingDownloadHandler(new File(".")), "/");
            server.start();
            return server;
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
          
      }
      
      
      protected static HttpServer startUploader(){
        try {
            // create a basic server that listens on port specified by sysprop.upload_port.
            SystemProperties sysprop = new SystemProperties();
            String port = sysprop.get("upload_port");
            System.out.println("Starting Upload Service on Port: " + port);
            final HttpServer server = HttpServer.createSimpleServer("", new Integer(port));        
            final ServerConfiguration config = server.getServerConfiguration();

            // Map the path, /upload, to the NonBlockingUploadHandler
            config.addHttpHandler(new NonBlockingUploadHandler(), "/ps/upload");

            try {
                server.start();
    //            LOGGER.info("Press enter to stop the server...");
    //            System.in.read();
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
            } finally {
    //            server.stop();
            }  

            return server;
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
      }
      
      protected static int writePID(BufferedWriter out){
        try {
            String pidstring = ManagementFactory.getRuntimeMXBean().getName();
            String[] split = pidstring.split("@");
            Integer p = new Integer(split[0]);
            System.out.println("PID: " + p);
            out.write(p.toString());
            out.close();          
            return p;
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
      }
      
      public static void main(String[] args) throws IOException {
          HttpServer httpServer = startServer();
          System.out.print("Executing from: ");
          System.out.println( new File(".").getCanonicalPath());
          System.out.println(String.format("Jersey app started with WADL available at "
                  + "%sapplication.wadl\nTry out %sps/status\nHit control-C to stop it...",
                  BASE_URI, BASE_URI));
          writePID(new BufferedWriter( new FileWriter("service.pid") ));
          int neverstop = 5;
          while (neverstop > 2 ){
              neverstop = 6;
            try {
                Thread.sleep(9999999000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
          httpServer.stop();
      }  
      
      
    
}
