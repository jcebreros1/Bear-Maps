/**
 * Created by jesuscebreros on 4/10/16.
 */
public class QTreeNode implements Comparable<QTreeNode> {
    private String id;
    private double upperLeftLatitude, upperLeftLongitude, lowerRightLatitude, lowerRightLongitude;
    protected QTreeNode first, second, third, fourth;
    private double dpp;

    public int compareTo(QTreeNode y) {
        //if y is after this turn 1 else return -1
        if (y.upperLeftLatitude() < this.upperLeftLatitude()) {
            return -1;
        } else if (y.upperLeftLatitude()
                > this.upperLeftLatitude()) {//check if it goes above
            return 1;
        } else if (y.upperLeftLongitude()
                > this.upperLeftLongitude())//check the x
        {
            return -1;
        }
        return 1;
    }

    public QTreeNode(String id, double upperLeftLatitude, double upperLeftLongitude,
                     double lowerRightLatitude, double lowerRightLongitude) {
        this.id = id;
        this.upperLeftLatitude = upperLeftLatitude;
        this.upperLeftLongitude = upperLeftLongitude;
        this.lowerRightLatitude = lowerRightLatitude;
        this.lowerRightLongitude = lowerRightLongitude;
        dpp = Math.abs((lowerRightLongitude
                - upperLeftLongitude) / 256);
    }

    public String getID() {
        return id;
    }

    public double upperLeftLatitude() {
        return upperLeftLatitude;
    }

    public double upperLeftLongitude() {
        return upperLeftLongitude;
    }

    public double lowerRightLatitude() {
        return lowerRightLatitude;
    }

    public double lowerRightLongitude() {
        return lowerRightLongitude;
    }

    public double dpp() {
        return dpp;
    }


    public double nextLatitude() {
        return (upperLeftLatitude
                + lowerRightLatitude) / 2.0;
    }

    public double nextLongitude() {
        return (lowerRightLongitude
                + upperLeftLongitude) / 2.0;
    }


    @Override
    public String toString() {
        return "QTreeNode{"
                + "lowerRightLongitude="
                + lowerRightLongitude
                + ", id='" + id + '\''
                + ", upperLeftLatitude="
                + upperLeftLatitude
                + ", upperLeftLongitude="
                + upperLeftLongitude
                + ", lowerRightLatitude="
                + lowerRightLatitude
                + '}';
    }
}
