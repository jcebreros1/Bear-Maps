import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashMap;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 *
 * @author Alan Yao
 */
public class GraphDB {
    /**
     * Example constructor shows how to create and start an XML parser.
     *
     * @param db_path Path to the XML file to be parsed.
     */
    HashMap<String, Node> dirtyPlaces;
    HashMap<String, Node> cleanPlaces = new HashMap<>();

    public GraphDB(String db_path) {
        try {
            File inputFile = new File(db_path);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
            dirtyPlaces = maphandler.getHashMap();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    public HashMap getCleanHasMap() {
        return cleanPlaces;
    }

    public HashMap getDirtyHashMap() {
        return dirtyPlaces;
    }

    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        for (Node s : dirtyPlaces.values()) {
            if (s.getEdge().size() > 0) {
                cleanPlaces.put(s.id, s);
            }
        }
    }
}
