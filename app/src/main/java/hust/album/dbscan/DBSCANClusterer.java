package hust.album.dbscan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class DBSCANClusterer<T> {
    /**
     * maximum distance of values to be considered as cluster
     */
    private double epsilon = 1f;

    /**
     * minimum number of members to consider cluster
     */
    private int minimumNumberOfClusterMembers = 2;

    /**
     * distance metric applied for clustering
     **/
    private DistanceMetric<T> metric = null;

    /**
     * internal list of input values to be clustered
     */
    private ArrayList<T> inputValues = null;

    /**
     * index maintaining visited points
     */
    private HashSet<Integer> visitedPoints = new HashSet<>();

    /**
     * Creates a DBSCAN clusterer instance.
     * Upon instantiation, call {@link #performClustering()}
     * to perform the actual clustering.
     *
     * @param inputValues    Input values to be clustered
     * @param minNumElements Minimum number of elements to constitute cluster
     * @param maxDistance    Maximum distance of elements to consider clustered
     * @param metric         Metric implementation to determine distance
     * @throws DBSCANClusteringException
     */
    public DBSCANClusterer(final Collection<T> inputValues, int minNumElements, double maxDistance, DistanceMetric<T> metric) throws DBSCANClusteringException {
        setInputValues(inputValues);
        setMinimalNumberOfMembersForCluster(minNumElements);
        setMaximalDistanceOfClusterMembers(maxDistance);
        setDistanceMetric(metric);
    }

    /**
     * Sets the distance metric
     *
     * @param metric
     * @throws DBSCANClusteringException
     */
    public void setDistanceMetric(final DistanceMetric<T> metric) throws DBSCANClusteringException {
        if (metric == null) {
            throw new DBSCANClusteringException("DBSCAN: Distance metric has not been specified (null).");
        }
        this.metric = metric;
    }

    /**
     * Sets a collection of input values to be clustered.
     * Repeated call overwrite the original input values.
     *
     * @param collection
     * @throws DBSCANClusteringException
     */
    public void setInputValues(final Collection<T> collection) throws DBSCANClusteringException {
        if (collection == null) {
            throw new DBSCANClusteringException("DBSCAN: List of input values is null.");
        }
        this.inputValues = new ArrayList<T>(collection);
    }

    /**
     * Sets the minimal number of members to consider points of close proximity
     * clustered.
     *
     * @param minimalNumberOfMembers
     */
    public void setMinimalNumberOfMembersForCluster(final int minimalNumberOfMembers) {
        this.minimumNumberOfClusterMembers = minimalNumberOfMembers;
    }

    /**
     * Sets the maximal distance members of the same cluster can have while
     * still be considered in the same cluster.
     *
     * @param maximalDistance
     */
    public void setMaximalDistanceOfClusterMembers(final double maximalDistance) {
        this.epsilon = maximalDistance;
    }

    /**
     * Determines the neighbours of a given input value.
     *
     * @param inputValue Input value for which neighbours are to be determined
     * @return list of neighbours
     * @throws DBSCANClusteringException
     */
    private ArrayList<Integer> getNeighbours(final T inputValue) throws DBSCANClusteringException {
        ArrayList<Integer> neighbours = new ArrayList<>();
        for (int i = 0; i < inputValues.size(); i++) {
            T candidate = inputValues.get(i);
            if (metric.calculateDistance(inputValue, candidate) <= epsilon) {
                neighbours.add(i);
            }
        }
        return neighbours;
    }

    /**
     * Merges the elements of the right collection to the left one and returns
     * the combination.
     *
     * @param neighbours1 left collection
     * @param neighbours2 right collection
     */
    private void mergeRightToLeftCollection(final ArrayList<Integer> neighbours1,
                                            final ArrayList<Integer> neighbours2) {
        for (int i = 0; i < neighbours2.size(); i++) {
            Integer tempPt = neighbours2.get(i);
            if (!neighbours1.contains(tempPt)) {
                neighbours1.add(tempPt);
            }
        }
    }


    /**
     * Applies the clustering and returns a collection of clusters (i.e. a list
     * of lists of the respective cluster members).
     *
     * @return
     * @throws DBSCANClusteringException
     */
    public ArrayList<ArrayList<Integer>> performClustering() throws DBSCANClusteringException {

        if (inputValues == null) {
            throw new DBSCANClusteringException("DBSCAN: List of input values is null.");
        }

        if (inputValues.isEmpty()) {
            throw new DBSCANClusteringException("DBSCAN: List of input values is empty.");
        }

        if (inputValues.size() < 2) {
            throw new DBSCANClusteringException("DBSCAN: Less than two input values cannot be clustered. Number of input values: " + inputValues.size());
        }

        if (epsilon < 0) {
            throw new DBSCANClusteringException("DBSCAN: Maximum distance of input values cannot be negative. Current value: " + epsilon);
        }

        if (minimumNumberOfClusterMembers < 2) {
            throw new DBSCANClusteringException("DBSCAN: Clusters with less than 2 members don't make sense. Current value: " + minimumNumberOfClusterMembers);
        }

        ArrayList<ArrayList<Integer>> resultList = new ArrayList<>();
        visitedPoints.clear();

        ArrayList<Integer> neighbours;

        for (int i = 0; i < inputValues.size(); i++) {
            T p = inputValues.get(i);
            if (!visitedPoints.contains(i)) {
                visitedPoints.add(i);
                neighbours = getNeighbours(p);
                if (neighbours.size() >= minimumNumberOfClusterMembers) {
                    for (int j = 0; j < neighbours.size(); j++) {
                        int neighbourId = neighbours.get(j);
                        T r = inputValues.get(neighbourId);
                        if (!visitedPoints.contains(neighbourId)) {
                            visitedPoints.add(neighbourId);
                            ArrayList<Integer> individualNeighbours = getNeighbours(r);
                            if (individualNeighbours.size() >= minimumNumberOfClusterMembers) {
                                mergeRightToLeftCollection(
                                        neighbours,
                                        individualNeighbours);
                            }
                        }
                    }
                    resultList.add(neighbours);
                }
            }
        }
        return resultList;
    }
}
