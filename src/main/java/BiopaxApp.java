// Import required java libraries


import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Protein;
import org.json.JSONObject;
import org.sbgn.bindings.Sbgn;

import javax.xml.bind.JAXBException;
import java.io.*;

import static java.lang.System.out;
import static java.lang.System.setOut;

public class BiopaxApp {

    public static void main(String[] args) throws IOException, JAXBException {

        SBGNPDToL3Converter conv = new SBGNPDToL3Converter();

        //String nodeInfo = "{id: \"abc\"}";
        //String out = conv.addNode(nodeInfo);

        //File fIn = new File("src/main/resources/testFile.xml");
        File fIn = new File("src/main/resources/logicSBGNLong.xml");
        InputStream in = new FileInputStream(fIn);

        File fOut = new File("src/main/resources/testOut.owl");
        FileOutputStream out = new FileOutputStream(fOut);
        conv.writeL3(in, out) ;
    }
}