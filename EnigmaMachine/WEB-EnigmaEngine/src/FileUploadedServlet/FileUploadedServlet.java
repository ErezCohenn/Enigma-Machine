package FileUploadedServlet;

import Engine.JaxbToMachineTransformer;
import EnigmaMachineException.BruteForceInProgressException;
import EnigmaMachineException.GeneralEnigmaMachineException;
import EnigmaMachineException.IllegalAgentsAmountException;
import EnigmaMachineException.MachineNotExistsException;
import Jaxb.Schema.Generated.CTEEnigma;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import servletUtils.ServletUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collection;

import static Constants.ServletConstants.USERNAME;
import static Utils.Constants.GSON_INSTANCE;

@WebServlet(name = "FileUploadedServlet", urlPatterns = "/fileUploaded")
@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 2, // 2MB
        maxFileSize = 1024 * 1024 * 10, // 10MB
        maxRequestSize = 1024 * 1024 * 50) // 50MB
public class FileUploadedServlet extends HttpServlet {

    @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
     response.setContentType("application/json");
     response.setCharacterEncoding("UTF-8");
     PrintWriter out = response.getWriter();
     synchronized (this){
        String name = request.getParameter(USERNAME); //SessionUtils.getUsername(request);
        name = name != null ? name : "Guest";


         Collection<Part> parts = request.getParts();
         for(Part part : parts) {
         InputStream inputStream = part.getInputStream();
         JaxbToMachineTransformer jaxbToMachineTransformer = new JaxbToMachineTransformer();
         try {
             CTEEnigma cteEnigma = jaxbToMachineTransformer.deserializeFrom(inputStream);
             if (ServletUtils.getUBoatManager(request.getServletContext()).isFileExists(cteEnigma.getCTEBattlefield().getBattleName())|| name.equals("Guest")) {
                 response.setStatus(HttpServletResponse.SC_CONFLICT);
                 if(name.equals("Guest")) {
                     System.out.println("didnt got the session");

                 }
                 else {
                     String jsonAlreadyUploadedMessage = "The battle is already uploaded" + " by: " + ServletUtils.getUBoatManager(request.getServletContext()).getUBoatByBattleName(cteEnigma.getCTEBattlefield().getBattleName());
                     response.getWriter().println(GSON_INSTANCE.toJson(jsonAlreadyUploadedMessage));
                     response.getWriter().flush();
                 }

             } else {
                 ServletUtils.getEngineManager(request.getServletContext()).setMachineDetailsFromXmlFile(cteEnigma);
                 ServletUtils.getUBoatManager(request.getServletContext()).addUBoat(name, ServletUtils.getEngineManager(request.getServletContext()).getCurrentEnigmaMachine()
                         , cteEnigma.getCTEBattlefield(), ServletUtils.getEngineManager(request.getServletContext()).getDictionaryObject());
                 response.setStatus(HttpServletResponse.SC_OK);

                 out.println(GSON_INSTANCE.toJson("File uploaded successfully by" + " " + name));
             }
         } catch (GeneralEnigmaMachineException | JAXBException | IllegalAgentsAmountException |
                  MachineNotExistsException | CloneNotSupportedException | BruteForceInProgressException e) {
             response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
             out.println(GSON_INSTANCE.toJson(e.getMessage()));
         }
     }
     }
    }
}
