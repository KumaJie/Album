package hust.album.dbscan.metric;


import hust.album.entity.Image;
import hust.album.dbscan.DistanceMetric;

public class DistanceMetricImage implements DistanceMetric<Image> {

    // 地球半径（单位：米）
    private final double EARTH_RADIUS = 6371000;

    /**
     * 计算两个经纬度点之间的距离
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 两点之间的距离（单位：米）
     */
    private double gpsDistance(double lat1, double lon1, double lat2, double lon2) {
        // 将经纬度转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // 计算纬度和经度的差值
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // 半正矢公式
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 计算距离
        double distance = EARTH_RADIUS * c;

        return distance;
    }

    /**
     * 计算两个时间戳之间的时间差
     *
     * @param time1 第一个时间戳
     * @param time2 第二个时间戳
     * @return 两个时间戳之间的时间差（单位：分钟）
     */
    private long timeDistance(long time1, long time2) {
        return Math.abs(time1 - time2) / 1000 / 60;
    }

    /**
     * 汉明距离
     *
     * @param x 64位的长整数
     * @param y 64位的长整数
     * @return 差异数量
     */
    public int hammingDistance(long x, long y) {
        int ans = 0;
        long xor = x ^ y; // 异或运算找出不同的位

        while (xor != 0) {
            ans += (xor & 1); // 检查最低位是否为1
            xor >>>= 1; // 将xor右移一位
        }

        return ans;
    }

    @Override
    public double calculateDistance(Image image1, Image image2) {
//        if (image1.getPhash() == 0 || image2.getPhash() == 0) {
//            return Double.MAX_VALUE;
//        }
//        if (hammingDistance(image1.getPhash(), image2.getPhash()) > 15) {
//            return Double.MAX_VALUE;
//        }
        if (image1.getLatitude() == 0 || image1.getLongitude() == 0) {
            return Double.MAX_VALUE;
        }
        if (image2.getLatitude() == 0 || image2.getLongitude() == 0) {
            return Double.MAX_VALUE;
        }
//        if (timeDistance(image1.getDate(), image2.getDate()) < 1) {
//            return 0;
//        }
        return gpsDistance(image1.getLatitude(), image1.getLongitude(), image2.getLatitude(), image2.getLongitude());
    }
}
