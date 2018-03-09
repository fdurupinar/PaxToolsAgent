/*
 * Paxtools Servlet to convert between SBGN and Biopax
 * Author: Funda Durupinar Babur<f.durupinar@gmail.com>
 */


import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;

import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.io.sbgn.ListUbiqueDetector;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.query.QueryExecuter;
import org.biopax.paxtools.query.algorithm.Direction;
import org.json.JSONObject;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.bind.JAXBException;

// Extend HttpServlet class
public class PaxtoolsServlet extends HttpServlet {

    private Model level3;

    static BioPAXIOHandler handler =  new SimpleIOHandler();
    SBGNPDToL3Converter sbgnpdToL3Converter;


    /**
     *  Creates an empty L3 BioPAX model
     * @throws ServletException
     */
    public void init() throws ServletException
    {
        BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();
        level3 = factory.createModel(); //create an empty model
        sbgnpdToL3Converter = new SBGNPDToL3Converter();
    }

    /**
     * Updates the level3 model
     * @param in InputStream containing owl file in byte array
     * @return sbgn String
     * @throws IOException
     */
    public String convertToSBGN(InputStream in) throws IOException {


        OutputStream out = new ByteArrayOutputStream();

        //Model
        level3 = handler.convertFromOWL(in);

        Set<String> blacklist = new HashSet<String>(Arrays.asList(
                "http://pid.nci.nih.gov/biopaxpid_685", "http://pid.nci.nih.gov/biopaxpid_678",
                "http://pid.nci.nih.gov/biopaxpid_3119", "http://pid.nci.nih.gov/biopaxpid_3114"));

        System.out.println("level3.getObjects().size() = " + level3.getObjects().size());


        L3ToSBGNPDConverter conv = new L3ToSBGNPDConverter(
                new ListUbiqueDetector(blacklist), null, true);

        conv.writeSBGN(level3, out);


        return out.toString();
    }


    /***
     * Handles requests
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set response content type
        response.setContentType("text/html");

        String resultStr ="";

        if(request.getParameter("reqType").contains("sbgn")) {
            InputStream in = new ByteArrayInputStream(request.getParameter("content").getBytes("UTF-8"));
            resultStr = convertToSBGN(in);

        }
        else if(request.getParameter("reqType").contains("biopax")) {//convert to biopax
            InputStream in = new ByteArrayInputStream(request.getParameter("content").getBytes("UTF-8"));
            OutputStream out = new ByteArrayOutputStream();

            try {
                sbgnpdToL3Converter.writeL3(in, out);
            }
            catch (JAXBException e) {
                e.printStackTrace();
            }


            resultStr = out.toString();

        }
        else if(request.getParameter("reqType").contains("partialBiopax")) {//convert to biopax
            InputStream in = new ByteArrayInputStream(request.getParameter("content").getBytes("UTF-8"));
            OutputStream out = new ByteArrayOutputStream();

            try {
                SBGNPDToL3Converter conv = new SBGNPDToL3Converter(); //get a new converter
                conv.writeL3(in, out);

            }
            catch (JAXBException e) {
                e.printStackTrace();
            }

            resultStr = out.toString();
        }


        PrintWriter outPrint = response.getWriter();

        System.out.println(resultStr);
        outPrint.println(resultStr);



    }

    public void destroy()
    {
        // do nothing.
    }
}