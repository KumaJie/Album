package hust.album.dbscan;

public interface DistanceMetric<T> {
    public double calculateDistance(T val1, T val2) throws DBSCANClusteringException;
}
