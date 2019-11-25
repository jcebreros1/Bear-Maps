import java.util.ArrayList;

/**
 * Created by jesuscebreros on 4/10/16.
 */
public class Node implements Comparable<Node> {
    String id;
    double lat, lon;
    private ArrayList<Node> edge;
    double distanceSoFar;
    private double priority;

    @Override
    public int compareTo(Node x) {
        int prio = Double.compare(this.priority, x.priority);
        if (prio == 0) {
            prio = Double.compare(this.priority
                    - this.distanceSoFar, x.priority - x.distanceSoFar);
        }
        return prio;
    }

    public Node(String id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        edge = new ArrayList<Node>();
    }

    public void setPriority(Node end) {
        this.priority = MapServer.euclenianDistance(this, end) + distanceSoFar;
    }

    public String getID() {
        return id;
    }

    public void setEdge(Node e) {
        edge.add(e);

    }

    public ArrayList<Node> getEdge() {
        return edge;
    }

    @Override
    public String toString() {
        return "Node{"
                + "id='"
                + id
                + '\''
                + ", lat="
                + lat
                + ", lon="
                + lon
                + ", edge="
                + edge.size()
                + '}';
    }
}
