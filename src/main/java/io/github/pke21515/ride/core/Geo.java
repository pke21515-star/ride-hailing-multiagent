package io.github.pke21515.ride.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置地理编码兜底：客户端未传经纬度（0,0）时，按地名查地标表；
 * 未命中则用地名哈希在市中心附近生成确定性伪坐标（同一地名结果稳定）。
 * 生产替换为高德/百度地理编码 API，仅改本类。
 */
public final class Geo {

    /** 郑州市中心（二七广场附近） */
    private static final double CITY_LAT = 34.7535;
    private static final double CITY_LNG = 113.6654;

    private static final Map<String, double[]> LANDMARKS = new LinkedHashMap<>();
    static {
        LANDMARKS.put("郑州东站", new double[]{34.7540, 113.7750});
        LANDMARKS.put("二七广场", new double[]{34.7535, 113.6600});
        LANDMARKS.put("郑东新区", new double[]{34.7690, 113.7300});
        LANDMARKS.put("CBD", new double[]{34.7690, 113.7300});
        LANDMARKS.put("新郑机场", new double[]{34.5280, 113.8490});
        LANDMARKS.put("机场", new double[]{34.5280, 113.8490});
        LANDMARKS.put("火车站", new double[]{34.7460, 113.6580});
        LANDMARKS.put("高新区", new double[]{34.7970, 113.5530});
        LANDMARKS.put("大学城", new double[]{34.7900, 113.8100});
        LANDMARKS.put("龙子湖", new double[]{34.7990, 113.8140});
        LANDMARKS.put("金融岛", new double[]{34.7890, 113.7350});
        LANDMARKS.put("奥体中心", new double[]{34.7310, 113.5960});
        LANDMARKS.put("碧沙岗", new double[]{34.7540, 113.6340});
        LANDMARKS.put("紫荆山", new double[]{34.7620, 113.6800});
    }

    private Geo() {}

    /** 返回 [lat, lng]：坐标有效则原样返回，否则查表/伪坐标兜底。 */
    public static double[] resolve(String name, double lat, double lng) {
        if (isValid(lat, lng)) return new double[]{lat, lng};
        String key = name == null ? "" : name.trim();
        for (Map.Entry<String, double[]> e : LANDMARKS.entrySet()) {
            if (key.contains(e.getKey())) return e.getValue();
        }
        // 确定性伪坐标：地名哈希 → 市中心 ±0.05°（约 ±5.5km）
        long h = key.isEmpty() ? 0 : key.hashCode();
        double dLat = ((h % 1000) / 1000.0 - 0.5) * 0.10;
        double dLng = (((h / 1000) % 1000) / 1000.0 - 0.5) * 0.10;
        return new double[]{CITY_LAT + dLat, CITY_LNG + dLng};
    }

    private static boolean isValid(double lat, double lng) {
        // 粗判：中国境内坐标视为有效（排除未传的 0,0 与异常值）
        return Math.abs(lat) > 1e-6 && Math.abs(lng) > 1e-6
                && lat > 3 && lat < 54 && lng > 73 && lng < 136;
    }
}
