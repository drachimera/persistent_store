/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.net.UnknownHostException;
import java.util.Set;

/**
 *
 * @author Daniel J. Quest
 */

// The Java class will be hosted at the URI path "/example"
@Path("/ps/status")
public class ExampleResource {
    

    // The Java method will process HTTP GET requests
    @GET 
    // The Java method will produce content identified by the MIME Media
    // type "text/plain"
    @Produces("application/json")
    public String getClichedMessage() {
        // Return some cliched textual content        
        return "{\n status : \"PSREST Server Up and Functioning\"\n}\n";
    }
    
}
    
