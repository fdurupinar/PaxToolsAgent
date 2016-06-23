// Import required java libraries


import com.hp.hpl.jena.tdb.store.Hash;
import org.apache.jena.atlas.lib.Cell;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.json.JSONObject;
import org.sbgn.SbgnUtil;
import org.sbgn.bindings.*;
import org.sbgn.bindings.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.*;


import static org.sbgn.ArcClazz.*;
import static org.sbgn.GlyphClazz.*;




public class SBGNPDToL3Converter  {

    private Model level3;

    private static BioPAXFactory factory = BioPAXLevel.L3.getDefaultFactory();

    private HashMap<String, Glyph> glyphMap;


    public SBGNPDToL3Converter(){
        level3 = factory.createModel(); //create an empty model
        glyphMap = new HashMap<String, Glyph>();
    }


    /**
     * A recursive method for converting glyphs into biopaxElements
     * @param parent root of glyphs
     * @param glyphs list of glyphs to convert
     */
    public void convertGlyphs(Glyph parent, List<Glyph> glyphs){

        if(glyphs.isEmpty())
            return;

        Complex cx = null;
        if(parent!=null && parent.getClazz().equals(COMPLEX.getClazz()))
             cx = (Complex) level3.getByID(parent.getId());

        for (Glyph g : glyphs) {

            String clazz = g.getClazz();
            PhysicalEntity entity = null;

            //TODO: id conversion

            glyphMap.put(g.getId(), g);

            if(clazz.equals(MACROMOLECULE.getClazz())|| clazz.equals(MACROMOLECULE_MULTIMER.getClazz()))
                entity = level3.addNew(Protein.class, g.getId());

            else if(clazz.equals(NUCLEIC_ACID_FEATURE.getClazz()) || clazz.equals(NUCLEIC_ACID_FEATURE_MULTIMER.getClazz()))
                entity = level3.addNew(Dna.class, g.getId());

            else if(clazz.equals(SIMPLE_CHEMICAL.getClazz()) ||clazz.equals(SIMPLE_CHEMICAL_MULTIMER.getClazz()))
                entity = level3.addNew(SmallMolecule.class, g.getId());

            else if(clazz.equals(UNSPECIFIED_ENTITY.getClazz()) || clazz.equals(PERTURBING_AGENT.getClazz()))
                entity = level3.addNew(PhysicalEntity.class, g.getId());

            else if(clazz.equals(COMPLEX.getClazz()) || clazz.equals(COMPLEX_MULTIMER.getClazz()))
                entity = level3.addNew(Complex.class, g.getId());

           


            //change parent class according to child's unit of information
            else if(clazz.equals(UNIT_OF_INFORMATION.getClazz())){
                if(parent!=null){ //parent should not be null

                    Label label = g.getLabel();

                    if(label.getText().contains("dna")) {
                        BioPAXElement existingParent = level3.getByID(parent.getId());
                        BioPAXElement newParent =factory.create(Dna.class, parent.getId());
                        level3.replace(existingParent, newParent);

                    }
                    else if(label.getText().contains("rna")) {
                        BioPAXElement existingParent = level3.getByID(parent.getId());
                        BioPAXElement newParent =factory.create(Rna.class, parent.getId());
                        level3.replace(existingParent, newParent);
                    }

                }
            }

            //Handle state variables
            EntityFeature entityFeature = factory.create(ModificationFeature.class, g.getId());
            if(g.getState()!=null) {
                entity.addFeature(entityFeature);
            }




            //Handle complexes
            if(cx!=null && entity!=null) {
                Complex existingCx = cx;
                cx.addComponent(entity);
            }




            //Handle children
            convertGlyphs(g, g.getGlyph());

        }
    }


    /**
     * Set locations after all the glyphs are added
     * @param glyphs All the glyphs in the pathway
     */

    public void convertCompartments(List<Glyph> glyphs){
        
        for (Glyph g : glyphs) {

            BioPAXElement entity = level3.getByID(g.getId());
            if(entity instanceof PhysicalEntity)
            {
                Glyph loc = (Glyph)g.getCompartmentRef();
                if(loc!=null) {

                    CellularLocationVocabulary clv = factory.create(CellularLocationVocabulary.class, loc.getId());
                    ((PhysicalEntity) entity).setCellularLocation(clv);
                }
            }

        }

    }

    /**
     *
     * @param leftIds
     * @param rightIds
     * @param processId
     */
    public void addConversion(ArrayList<String> leftIds, ArrayList<String> rightIds, String processId){

        Conversion cnv = factory.create(Conversion.class,processId);

        cnv.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);

        for(String id:leftIds)
            cnv.addLeft((PhysicalEntity)level3.getByID(id));

        for(String id:rightIds)
            cnv.addRight((PhysicalEntity)level3.getByID(id));


        if(!level3.contains(cnv))
            level3.add(cnv);
    }
    public void addCatalysis(ArrayList<String> leftIds, ArrayList<String> rightIds, String processId) {


        Catalysis cat = factory.create(Catalysis.class,processId);

        cat.setCatalysisDirection(CatalysisDirectionType.LEFT_TO_RIGHT);

        for(String id:leftIds)
            cat.addController((PhysicalEntity)level3.getByID(id));
        for(String id:rightIds)
            cat.addController((PhysicalEntity)level3.getByID(id));

        if(!level3.contains(cat))
            level3.add(cat);
    }
    public void addControl(ArrayList<String> leftIds, ArrayList<String> rightIds, String processId, ControlType controlType) {


        Control ctrl = factory.create(Control.class,processId);

        for(String id:leftIds)
            ctrl.addController((PhysicalEntity)level3.getByID(id));
        for(String id:rightIds)
            ctrl.addController((PhysicalEntity)level3.getByID(id));

        ctrl.setControlType(controlType);

        if(!level3.contains(ctrl))
            level3.add(ctrl);
    }

    public void addComplexAssembly(ArrayList<String> leftIds, ArrayList<String> rightIds, String processId) {


        ComplexAssembly ca = factory.create(ComplexAssembly.class,processId);
        ca.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);

        for(String id:leftIds)
            ca.addLeft((PhysicalEntity)level3.getByID(id));

        for(String id:rightIds)
            ca.addRight((PhysicalEntity)level3.getByID(id));


        if(!level3.contains(ca))
            level3.add(ca);
    }

    public void addProcess(String arcClazz, ArrayList<String> sourceIds, ArrayList<String> targetIds){


        String processId = "";
        for(String s: sourceIds)
            processId += s + "-";
        for(String s: targetIds)
            processId += s + "-";


        if(arcClazz.equals(PRODUCTION.getClazz()))
            addConversion(sourceIds, targetIds, processId);

        else if ( arcClazz.equals(CONSUMPTION.getClazz()))
            addConversion(targetIds, sourceIds, processId); //swap directions of source and target

        else if(arcClazz.equals(CATALYSIS.getClazz()))
            addCatalysis(sourceIds, targetIds, processId);

        else if(arcClazz.equals(INHIBITION.getClazz()))
            addControl(sourceIds, targetIds, processId, ControlType.INHIBITION);

        else if(arcClazz.equals(STIMULATION.getClazz()) ||arcClazz.equals(NECESSARY_STIMULATION.getClazz()) )
            addControl(sourceIds, targetIds,processId, ControlType.ACTIVATION);

        else if(arcClazz.equals(MODULATION.getClazz()))
            addControl(sourceIds, targetIds, processId, null);

        else if(arcClazz.equals(ASSOCIATION.getClazz()) || arcClazz.equals(DISSOCIATION.getClazz()) )
            addComplexAssembly(sourceIds, targetIds, processId);

    }

    /**
     * Check if  arc or glyph is not logical
     * @param clazz
     * @return
     */
    public boolean isNonLogicalElement(String clazz){
        if(!clazz.equals(AND.getClazz()) && !clazz.equals(OR.getClazz()) && !clazz.equals(NOT.getClazz()) && !clazz.equals(LOGIC_ARC.getClazz()))
            return true;

        /*if(clazz.equals(MACROMOLECULE.getClazz()) || clazz.equals(UNSPECIFIED_ENTITY.getClazz()) || clazz.equals(SIMPLE_CHEMICAL.getClazz()) ||
                clazz.equals(NUCLEIC_ACID_FEATURE.getClazz()) || clazz.equals(SIMPLE_CHEMICAL_MULTIMER.getClazz()) || clazz.equals(SIMPLE_CHEMICAL_MULTIMER.getClazz()) ||
                clazz.equals(MACROMOLECULE_MULTIMER.getClazz()) || clazz.equals(PERTURBING_AGENT.getClazz()) || clazz.equals(SIMPLE_CHEMICAL_MULTIMER.getClazz()) ||
                clazz.equals(NUCLEIC_ACID_FEATURE_MULTIMER.getClazz()) || clazz.equals(COMPLEX.getClazz()) || clazz.equals(COMPLEX_MULTIMER.getClazz()))
            return true;
*/
        return false;

    }

    /**
     * Find arcs connected to glyp g except current arc
     * @param g
     * @param currentArc
     * @param arcs List of all the arcs
     * @return
     */

    public ArrayList<Arc> findConnectedArcs(Glyph g, Arc currentArc, List<Arc> arcs) {


        ArrayList<Arc> connectedArcs = new ArrayList<>();

        for(Arc a: arcs) {
            if(!a.equals(currentArc) && ((a.getSource()).equals(g) ||   (a.getTarget()).equals(g))) { //skip current arc

                connectedArcs.add(a);
            }
        }

        return connectedArcs;
    }

    public ArrayList<ArrayList<String>> distribute(ArrayList<ArrayList<String>> a1, ArrayList<ArrayList<String>> a2){
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();


        if(a1.size() == 1){
            for(ArrayList<String> el: a2){
                el.addAll(a1.get(0));
            }

            result.addAll(a2);
          //  System.out.println("a1= " + a1 + " a2= " + a2 + " result= " + result);
            return result;
        }


        if(a1.isEmpty()) {
            if (a2.isEmpty())
                return result;
            else
                return a2;
        }
        else if(a2.isEmpty())
            return a1;


        ArrayList<String> headEl = a1.remove(0); //a1 is smaller now

        ArrayList<ArrayList<String>> headList = new ArrayList<ArrayList<String>>();
        headList.add(headEl);



        //Make a copy of a2 before adding everyghing
        ArrayList<ArrayList<String>> a2Temp = new ArrayList<ArrayList<String>>();
        for(ArrayList<String> el: a2){
            ArrayList<String> elCopy = new ArrayList<String>(el);

            a2Temp.add(elCopy);
        }


        ArrayList<ArrayList<String>> headResult = distribute(headList,a2);

        result = distribute(a1,a2Temp);

        result.addAll(headResult);


        return result;

    }
    /**
     * Finds participants of an AND/OR relationship
     * rootG's children are connected with AND or OR
     * @param rootG  can be AND, OR, molecule, non-molecule or null
     *
     * @return A list of glyph-lists connected by ORs. Inner glyph list elements are connected by ANDs.
     */
    public ArrayList<ArrayList<String>> getParticipants(Glyph rootG, Arc currentArc, List<Arc> arcs) {


        ArrayList<ArrayList<String>> orParticipants = new ArrayList<ArrayList<String>>();


        if (rootG == null)
            return orParticipants;

        String clazz = rootG.getClazz();


        if (isNonLogicalElement(clazz)) {
            ArrayList<String> andParticipants = new ArrayList<>();
            andParticipants.add(rootG.getId());
            orParticipants.add(andParticipants);
            return orParticipants;
        }
        else {
            if (clazz.equals(OR.getClazz())) {

                ArrayList<Arc> connectedArcs = findConnectedArcs(rootG, currentArc, arcs);

                for (Arc a1:connectedArcs)
                    System.out.println("and root= " + rootG.getId() +  " connectedArcs = " + a1.getId() + " currentArc: " + currentArc.getId());

                for (Arc a: connectedArcs) {
                  //  System.out.println("a.getId() = " + a.getId() + " " + rootG.getId());
                    Glyph otherGlyph = ((Glyph)a.getSource()).equals(rootG) ? (Glyph)a.getTarget(): (Glyph)a.getSource();

                    ArrayList<ArrayList<String>> orParticipantsChild = getParticipants(otherGlyph, a, arcs);

                    orParticipants.addAll(orParticipantsChild); //concat child lists
                }
            }
            else if (clazz.equals(AND.getClazz())) {

                ArrayList<Arc> connectedArcs = findConnectedArcs(rootG, currentArc,  arcs);


                //for printing only
                //for (Arc a1:connectedArcs)
                  //  System.out.println("or root= " + rootG.getId() +  " connectedArcs = " + a1.getId() + " currentArc: " + currentArc.getId());

                for (Arc a1:connectedArcs) {

                    Glyph otherGlyph = ((Glyph)a1.getSource()).equals(rootG) ? (Glyph) a1.getTarget() : (Glyph) a1.getSource();

                    ArrayList<ArrayList<String>> orParticipantsChild = getParticipants(otherGlyph, a1,  arcs);


                    if(orParticipants.isEmpty())
                        orParticipants.addAll(orParticipantsChild);

                    else {
                        orParticipants = distribute(orParticipantsChild, orParticipants);

                    }


                }


                }



        }
        return orParticipants;
    }

    public void convertArcs(List<Arc> arcs){
        for (Arc a : arcs) {

            String sourceId = ((Glyph)a.getSource()).getId(); //ids are the same in bp and sbgn
            String targetId = ((Glyph)a.getTarget()).getId(); //ids are the same in bp and sbgn

            String processId = sourceId + "-" + targetId;

            String arcClazz = a.getClazz();

            //Assign sources and targets of the process going top-down
            Glyph sourceGlyph = glyphMap.get(sourceId);
            Glyph targetGlyph = glyphMap.get(targetId);


            if(isNonLogicalElement(a.getClazz())) {

            //    System.out.println(sourceGlyph.getId() + "  " + targetGlyph.getId() );
                ArrayList<ArrayList<String>> sourceParticipants = getParticipants(sourceGlyph, a,  arcs);
                ArrayList<ArrayList<String>> targetParticipants = getParticipants(targetGlyph, a,  arcs);



            //    System.out.println(arcClazz + " sp: " + sourceParticipants.toString() + " tp: " + targetParticipants.toString());

                for (ArrayList<String> sp : sourceParticipants)
                    for (ArrayList<String> tp : targetParticipants) {
                        System.out.println("sp tp = " + sp + " " + tp);
                        addProcess(arcClazz, sp, tp);
                    }
            }

        }


    }

    /**
     *  Creates a biopax model file from sbgn
     * @param sbgn
     */
    public void createL3(Sbgn sbgn) {


        // map is a container for the glyphs and arcs
        Map map = sbgn.getMap();

        convertGlyphs(null, map.getGlyph());
        convertCompartments(map.getGlyph());
        convertArcs(map.getArc());


    }

    /**
     * Writes the contents of level3 into an .owl file
     * @param in Inputstream
     * @param out Outputstream
     * @throws JAXBException
     * @throws IOException
     */

    public void writeL3(InputStream in, OutputStream out) throws JAXBException, IOException {


        JAXBContext context = JAXBContext.newInstance("org.sbgn.bindings");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Sbgn sbgn = (Sbgn)unmarshaller.unmarshal(in);


        createL3(sbgn);


        SimpleIOHandler io = new SimpleIOHandler();
        io.convertToOWL(level3, out);
    }

    


    public String addNode(String nodeInfo){

        JSONObject jsonObj= new JSONObject(nodeInfo);



        String sbgnClass = jsonObj.getString("sbgnclass");
        String id = jsonObj.getString("id");

        if(sbgnClass.equalsIgnoreCase("macromolecule")) {
        //    String statesAndInfos = jsonObj.getString("sbgnStatesAndInfos");
            level3.addNew(Protein.class, id);
        }




        return "OK";
    }
    public String changeUnitOfInformation(String id, String unitOfInformation){

        //JSONObject jsonObj= new JSONObject();


        BioPAXElement existing = level3.getByID(id);

      //  BioPAXElement newEle  = existing;

       // System.out.println(newEle);
        //level3.replace(existing, )





        return "OK";
    }



}