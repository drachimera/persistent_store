/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package us.kbase.psrest.resources;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
//import org.jboss.resteasy.plugins.providers.multipart.InputPart;
//import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
 
@Path("/ps/file_upload")
public class UploadFileService {
 
	private final String UPLOADED_FILE_PATH = "/tmp";
 

        @POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
        public String uploadFile(InputStream uploadedInputStream){
            
            String uploadedFileLocation = "/tmp/harhar.txt";
            writeToFile(uploadedInputStream, uploadedFileLocation);
            String msg = "Upload File\n name: " +  "\n";
            System.out.println(msg);
            return msg;
        }
//	public Response uploadFile(
//		@FormDataParam("file") InputStream uploadedInputStream,
//		@FormDataParam("file") FormDataContentDisposition fileDetail) {
// 
//		String uploadedFileLocation = "/tmp/" + fileDetail.getFileName();
// 
//		// save it
//		writeToFile(uploadedInputStream, uploadedFileLocation);
// 
//		String output = "File uploaded to : " + uploadedFileLocation;
// 
//		return Response.status(200).entity(output).build();
// 
//	}
 
	// save uploaded file to new location
	private void writeToFile(InputStream uploadedInputStream,
		String uploadedFileLocation) {
 
		try {
			OutputStream out = new FileOutputStream(new File(
					uploadedFileLocation));
			int read = 0;
			byte[] bytes = new byte[1024];
 
			out = new FileOutputStream(new File(uploadedFileLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
 
			e.printStackTrace();
		}
 
	}
        
}
