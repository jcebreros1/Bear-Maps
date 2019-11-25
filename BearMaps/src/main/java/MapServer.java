import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import com.google.gson.Gson;

import java.io.*;
import java.util.List;


import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 *
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /**
     * Each tile is 256x256 pixels.
     */
    public static final int TILE_SIZE = 256;
    /**
     * HTTP failed response.
     */
    private static final int HALT_RESPONSE = 403;
    /**
     * Route stroke information: typically roads are not more than 5px wide.
     */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /**
     * Route stroke information: Cyan with half transparency.
     */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /**
     * The tile images are in the IMG_ROOT folder.
     */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
            "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
            "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB graph;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        graph = new GraphDB(OSM_DB_PATH);
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     *
     * @param req            HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     * The rastered photo must have the following properties:
     * <ul>
     * <li>Has dimensions of at least w by h, where w and h are the user viewport width
     * and height.</li>
     * <li>The tiles collected must cover the most longitudinal distance per pixel
     * possible, while still covering less than or equal to the amount of
     * longitudinal distance per pixel in the query box for the user viewport size. </li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the
     * above condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     * ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     * </li>
     * </ul>
     * Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     *
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String,
            Double> params, OutputStream os) throws IOException {
        //File file = new File(IMG_ROOT);
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
//        QuadTree quadTree = new QuadTree(ROOT_ULLAT, ROOT_ULLON, ROOT_LRLAT, ROOT_LRLON);
//        ArrayList<QTreeNode> images = quadTree.find(params.get("w"),params.get("h"),params.get("ullat"), params.get("ullon"), params.get("lrlat"), params.get("lrlon"));
        QuadTree quadTree = new QuadTree(ROOT_ULLAT, ROOT_ULLON, ROOT_LRLAT, ROOT_LRLON);
        ArrayList<QTreeNode> images = quadTree.find(params.get("w"), params.get("h"),
                params.get("ullat"), params.get("ullon"), params.get("lrlat"), params.get("lrlon"));
//        for(QTreeNode image : images){
//            System.out.println(image.getID());
//        }
        //System.out.println(images.size());
        double yCoor = images.get(0).upperLeftLatitude();
        int width = 0;
        for (QTreeNode image : images) {
            if (image.upperLeftLatitude() != yCoor) {
                break;
            }
            width += 256;
        }
        int height = 0;
        int heightToAdd = (ImageIO.read(new File(IMG_ROOT
                + images.get(0).getID()
                + ".png"))).getHeight();
        height += heightToAdd;
        for (QTreeNode image : images) {
            if (image.upperLeftLatitude() != yCoor) {
                yCoor = image.upperLeftLatitude();
                height += heightToAdd;
            }
        }
        BufferedImage result = new BufferedImage(width,
                height, BufferedImage.TYPE_INT_RGB);
        Graphics g = result.getGraphics();
        int x = 0;
        int y = 0;
        for (QTreeNode image : images) {
            BufferedImage bi = ImageIO.read(new File(IMG_ROOT
                    + image.getID() + ".png"));
            g.drawImage(bi, x, y, null);
            x += 256;
            if (x >= result.getWidth()) {
                x = 0;
                y += bi.getHeight();
            }
        }

        Stroke stroke = new BasicStroke(ROUTE_STROKE_WIDTH_PX,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Graphics2D graphics2D = result.createGraphics();
        graphics2D.setStroke(stroke);
        graphics2D.setColor(ROUTE_STROKE_COLOR);

        if (!routNodes.isEmpty()) {
            for (int i = 0; i < routNodes.size() - 1; i++) {
                Node a = routNodes.get(i);
                Node b = routNodes.get(i + 1);
                double widthOfRoute = (images.get(images.size() - 1).lowerRightLongitude()
                        - images.get(0).upperLeftLongitude());
                double heightOfRoute = (images.get(0).upperLeftLatitude()
                        - images.get(images.size() - 1).lowerRightLatitude());
                int xStart = (int) (((a.lon
                        - images.get(0).upperLeftLongitude()) / widthOfRoute) * result.getWidth());
                int yStart = (int) (((images.get(0).upperLeftLatitude()
                        - a.lat) / heightOfRoute) * result.getHeight());
                int xEnd = (int) (((b.lon
                        - images.get(0).upperLeftLongitude()) / widthOfRoute) * result.getWidth());
                int yEnd = (int) (((images.get(0).upperLeftLatitude()
                        - b.lat) / heightOfRoute) * result.getHeight());
                graphics2D.drawLine(xStart, yStart, xEnd, yEnd);
            }
        }


        ImageIO.write(result, "png", os);
        rasteredImageParams.put("raster_ul_lon", images.get(0).upperLeftLongitude());//params.get("ullon"));
        rasteredImageParams.put("raster_ul_lat", images.get(0).upperLeftLatitude());//params.get("ullat"));
        rasteredImageParams.put("raster_lr_lon", images.get(images.size()
                - 1).lowerRightLongitude());//params.get("lrlon"));
        rasteredImageParams.put("raster_lr_lat", images.get(images.size()
                - 1).lowerRightLatitude());//params.get("lrlat"));
        rasteredImageParams.put("raster_width", width);//params.get("w"));//result.getWidth());
        rasteredImageParams.put("raster_height", height);//params.get("h"));//result.getHeight());
        rasteredImageParams.put("depth", images.get(0).getID().length());
        rasteredImageParams.put("query_success", true);

        return rasteredImageParams;
    }

    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     *
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    static LinkedList<Node> routNodes = new LinkedList<Node>();

    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        routNodes = new LinkedList<Node>();
        LinkedList<Long> shortesPath = new LinkedList<Long>();
        //distanceComparable compare = new distanceComparable(params.get("end_lat"), params.get("end_lon"));
        //System.out.println("Start Latitude "+params.get("start_lat") + params.get("start_lon"));
        Node StartNode = FindClosestNode(params.get("start_lat"),
                params.get("start_lon"));
        Node finalNode = FindClosestNode(params.get("end_lat"),
                params.get("end_lon"));
        HashSet<Node> visited = new HashSet<Node>(); //<- HashSet of nodes, used to track nodes we have already visited
        HashMap<String, Double> dist = new HashMap<String, Double>(); //<- HashMap of node to path distance to that node
        HashMap<String, Node> prev = new HashMap<String, Node>();//<- HashMap of previous pointers
        //PriorityQueue<Node> fringe = new PriorityQueue<Node>(10, compare); //<- PriorityQueue comparing on dist+heuristic
        PriorityQueue<Node> fringe = new PriorityQueue<Node>(); //<- PriorityQueue comparing on dist+heuristic

        //fringe.insert(StartNode);
        fringe.add(StartNode);
        dist.put(StartNode.getID(), 0.0);
        StartNode.distanceSoFar = 0;
        Node v;

        while (!fringe.isEmpty()) {
            //v = fringe.delMin();
            v = fringe.remove();
            if (visited.contains(v)) {
                continue;
            }
            visited.add(v);
            if (v.equals(finalNode)) {
                break;
            }// Found destination vertex and have valid prev pointers
            for (Node c : v.getEdge()) { // Do not check if your children have been visited
                //System.out.println(c);
//                if (visited.contains(c)){
//                    if (euclenianDistance(c,finalNode) < euclenianDistance(v,finalNode)){
//                        fringe.add(c);
//                        dist.put(c.getID(),dist.get(v.getID()) + euclenianDistance(v, c));
//                    }
//                }
                if (!dist.containsKey(c.getID()) ||
                        (dist.get(c.getID()) > dist.get(v.getID()) + euclenianDistance(v, c))) {
                    dist.put(c.getID(), dist.get(v.getID()) + euclenianDistance(v, c)); // Update distance
                    c.distanceSoFar = dist.get(v.getID()) + euclenianDistance(v, c);
                    c.setPriority(finalNode);
                    //fringe.insert(c);// Update pqueue
                    //visited.add(c);
                    fringe.add(c);// Update pqueue
                    //c.prev = v;
                    prev.put(c.getID(), v);// Update back-pointers
                }
            }
        }
        Node current = finalNode;
        shortesPath.add(Long.parseLong(current.getID()));
        routNodes.add(finalNode);
        while (!current.equals(StartNode)) {
            routNodes.addFirst(current);
            current = prev.get(current.getID());
            shortesPath.addFirst(Long.parseLong(current.id));
        }
        return shortesPath;
    }

    public static Node FindClosestNode(double start_lat, double start_lon) {
        //double withinDistance = .1;
        LinkedList<Double> closestNodeDistance = new LinkedList<Double>();
        LinkedList<Node> closestNode = new LinkedList<Node>();
        HashMap<String, Node> allNodes = new HashMap<String, Node>();
        allNodes = graph.getCleanHasMap();
        allNodes.size();
        for (Node s : allNodes.values()) {
            double distanceFromStartNode = Math.sqrt(Math.pow((start_lat
                    - s.lat), 2) + Math.pow((start_lon - s.lon), 2));
            if (closestNode.size() == 0) {
                closestNodeDistance.add(distanceFromStartNode);
                closestNode.add(s);
            } else {
                if (distanceFromStartNode < closestNodeDistance.getLast()) {
                    closestNodeDistance.removeLast();
                    closestNode.removeLast();
                    closestNodeDistance.add(distanceFromStartNode);
                    closestNode.add(s);

                }
            }
        }
        return closestNode.getLast();
    }

    public static double euclenianDistance(Node v, Node c) {
        return Math.sqrt(Math.pow((v.lat - c.lat), 2) + Math.pow((v.lon - c.lon), 2));
    }

    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return new LinkedList<>();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }
}

