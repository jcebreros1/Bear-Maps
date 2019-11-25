/**
 * Created by jesuscebreros on 4/10/16.
 */

import java.util.ArrayList;
import java.util.Collections;

public class QuadTree {
    private QTreeNode root;
    private ArrayList<QTreeNode> images;

    public QuadTree(double upperLeftLatitude, double upperLeftLongitude,
                    double lowerRightLatitude, double lowerRightLongitude) {
        root = new QTreeNode("root", upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude);
        images = new ArrayList<QTreeNode>();
        insert(root, "0", upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude);
    }

    public void insert(QTreeNode node, String id, double upperLeftLatitude,
                       double upperLeftLongitude, double lowerRightLatitude,
                       double lowerRightLongitude) {
        if (id.length() == 7) {
            return;
        }
        int tempID1 = Integer.parseInt(id) * 10 + 1;
        int tempID2 = Integer.parseInt(id) * 10 + 2;
        int tempID3 = Integer.parseInt(id) * 10 + 3;
        int tempID4 = Integer.parseInt(id) * 10 + 4;
        node.first = new QTreeNode(Integer.toString(tempID1), upperLeftLatitude,
                upperLeftLongitude, node.nextLatitude(), node.nextLongitude());
        node.second = new QTreeNode(Integer.toString(tempID2), upperLeftLatitude,
                node.nextLongitude(), node.nextLatitude(), lowerRightLongitude);
        node.third = new QTreeNode(Integer.toString(tempID3), node.nextLatitude(),
                upperLeftLongitude, lowerRightLatitude, node.nextLongitude());
        node.fourth = new QTreeNode(Integer.toString(tempID4), node.nextLatitude(),
                node.nextLongitude(), lowerRightLatitude, lowerRightLongitude);

        insert(node.first, Integer.toString(tempID1), upperLeftLatitude,
                upperLeftLongitude, node.nextLatitude(), node.nextLongitude());
        insert(node.second, Integer.toString(tempID2), upperLeftLatitude,
                node.nextLongitude(), node.nextLatitude(), lowerRightLongitude);
        insert(node.third, Integer.toString(tempID3), node.nextLatitude(),
                upperLeftLongitude, lowerRightLatitude, node.nextLongitude());
        insert(node.fourth, Integer.toString(tempID4), node.nextLatitude(),
                node.nextLongitude(), lowerRightLatitude, lowerRightLongitude);
    }

    public static void recurseThroughQuadtree(QTreeNode toRecurseThrough) {
        if (toRecurseThrough.first == null) {
            return;
        }
        System.out.println(toRecurseThrough.first.getID());
        System.out.println(toRecurseThrough.second.getID());
        System.out.println(toRecurseThrough.third.getID());
        System.out.println(toRecurseThrough.fourth.getID());
        recurseThroughQuadtree(toRecurseThrough.first);
        recurseThroughQuadtree(toRecurseThrough.second);
        recurseThroughQuadtree(toRecurseThrough.third);
        recurseThroughQuadtree(toRecurseThrough.fourth);
    }

    public int getDepthness(double height, double width, double upperLeftLatitude,
                            double upperLeftLongitude, double lowerRightLatitude,
                            double lowerRightLongitude) {
//        int n = 1;
//        QTreeNode temp = root.first;
//        //starts off only covering a certain level
//        double queryDPP =Math.abs((lowerRightLongitude - upperLeftLongitude) / (width));
//        System.out.println("queryDPP " + queryDPP);
//        //starts off covering everything
//        double currentDPP = (temp.lowerRightLongitude() - temp.upperLeftLongitude())/256;
//        System.out.println("currentDPP "+ currentDPP);
//        while (queryDPP < currentDPP && n != 7) {
//            n += 1;
//            temp = temp.first;
//            currentDPP = temp.dpp();
//            System.out.println("currentDPP "+ currentDPP);
//            System.out.println("Deptness "+n);
//        }
//        System.out.println("-----------");
        int dept = 1;
        double curentdpp = (MapServer.ROOT_LRLON
                - MapServer.ROOT_ULLON) / ((Math.pow(2, dept) * MapServer.TILE_SIZE));
        double qDDP = (lowerRightLongitude - upperLeftLongitude) / width;
        while (qDDP < curentdpp && dept != 7) {
            dept += 1;
            curentdpp = (MapServer.ROOT_LRLON
                    - MapServer.ROOT_ULLON) / ((Math.pow(2, dept) * MapServer.TILE_SIZE));
        }
        return dept;
    }

    public ArrayList find(double width, double height,
                          double upperLeftLatitude, double upperLeftLongitude,
                          double lowerRightLatitude, double lowerRightLongitude) {
        int depthnesslevel = getDepthness(height, width, upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
        getImagesWithinBoundAtCertainCoordinates(root, depthnesslevel, upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
        Collections.sort(images);
        return images;
    }

    public void getImagesWithinBoundAtCertainCoordinates
            (QTreeNode node, int deptness, double upperLeftLatitude,
             double upperLeftLongitude, double lowerRightLatitude, double lowerRightLongitude) {
        if (deptness == 1) {
            checkIfInbounds(node, upperLeftLatitude, upperLeftLongitude,
                    lowerRightLatitude, lowerRightLongitude);
            return;
        }
        getImagesWithinBoundAtCertainCoordinates(node.first, deptness - 1,
                upperLeftLatitude, upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
        getImagesWithinBoundAtCertainCoordinates(node.second, deptness - 1, upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
        getImagesWithinBoundAtCertainCoordinates(node.third, deptness - 1, upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
        getImagesWithinBoundAtCertainCoordinates(node.fourth, deptness - 1, upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);

    }

    public void checkIfInbounds(QTreeNode node, double upperLeftLatitude, double upperLeftLongitude,
                                double lowerRightLatitude, double lowerRightLongitude) {
        if (checkBounds(node.first, upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude)) {
            images.add(node.first);
        }
        if (checkBounds(node.second, upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude)) {
            images.add(node.second);
        }
        if (checkBounds(node.third, upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude)) {
            images.add(node.third);
        }
        if (checkBounds(node.fourth, upperLeftLatitude, upperLeftLongitude,
                lowerRightLatitude, lowerRightLongitude)) {
            images.add(node.fourth);
        }
    }

    public boolean checkBounds(QTreeNode node, double upperLeftLatitude,
                               double upperLeftLongitude,
                               double lowerRightLatitude,
                               double lowerRightLongitude) {
        return insideRectangle(node.upperLeftLongitude(),
                node.upperLeftLatitude(), upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude)
                || insideRectangle(node.lowerRightLongitude(),
                node.lowerRightLatitude(), upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude)
                || insideRectangle(node.lowerRightLongitude(),
                node.upperLeftLatitude(), upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude)
                || insideRectangle(node.upperLeftLongitude(),
                node.lowerRightLatitude(), upperLeftLatitude,
                upperLeftLongitude, lowerRightLatitude, lowerRightLongitude);
    }

    public boolean insideRectangle(double x, double y, double upperLeftLatitude,
                                   double upperLeftLongitude,
                                   double lowerRightLatitude,
                                   double lowerRightLongitude) {
        return x <= lowerRightLongitude && x >= upperLeftLongitude
                && y <= upperLeftLatitude && y >= lowerRightLatitude;
    }
}
