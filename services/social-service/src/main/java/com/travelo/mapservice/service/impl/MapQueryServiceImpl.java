package com.travelo.mapservice.service.impl;

import com.travelo.mapservice.dto.MapMediaItemDto;
import com.travelo.mapservice.dto.MapMediaResponseDto;
import com.travelo.mapservice.service.MapQueryService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MapQueryServiceImpl implements MapQueryService {

    private final NamedParameterJdbcTemplate jdbc;

    public MapQueryServiceImpl(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MapMediaResponseDto query(
            String userId,
            String mode,
            String bbox,
            double zoom,
            String mediaType,
            String collectionId,
            String tripId,
            String timeRange
    ) {
        double[] b = parseBbox(bbox);
        boolean cluster = zoom < 11.0d;
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("minLng", b[0])
                .addValue("minLat", b[1])
                .addValue("maxLng", b[2])
                .addValue("maxLat", b[3])
                .addValue("grid", clusterGrid(zoom))
                .addValue("collectionId", emptyToNull(collectionId))
                .addValue("tripId", emptyToNull(tripId))
                .addValue("mediaType", emptyToNull(mediaType))
                .addValue("recentFrom", recentFrom(timeRange));

        List<MapMediaItemDto> items = cluster ? queryClusters(p) : queryMedia(p);
        return new MapMediaResponseDto(mode == null ? "collections" : mode, zoom, items.size(), items);
    }

    private List<MapMediaItemDto> queryClusters(MapSqlParameterSource p) {
        final String sql = """
                SELECT
                  CONCAT('cluster:', ROUND(ST_Y(ST_Centroid(ST_Collect(cm.geom::geometry)))::numeric,4), ':', ROUND(ST_X(ST_Centroid(ST_Collect(cm.geom::geometry)))::numeric,4)) AS id,
                  COUNT(*)::int AS count,
                  ST_Y(ST_Centroid(ST_Collect(cm.geom::geometry))) AS lat,
                  ST_X(ST_Centroid(ST_Collect(cm.geom::geometry))) AS lng,
                  MAX(COALESCE(cm.thumbnail_url, cm.media_url)) AS thumbnail_url
                FROM collection_media cm
                JOIN collections c ON c.id = cm.collection_id
                WHERE c.user_id = :userId
                  AND cm.geom IS NOT NULL
                  AND ST_Intersects(cm.geom::geometry, ST_MakeEnvelope(:minLng,:minLat,:maxLng,:maxLat,4326))
                  AND (:collectionId IS NULL OR c.id::text = :collectionId)
                  AND (:tripId IS NULL OR c.trip_id = :tripId)
                  AND (:mediaType IS NULL OR UPPER(cm.media_type) = UPPER(:mediaType))
                  AND (:recentFrom IS NULL OR cm.captured_at >= :recentFrom)
                GROUP BY ST_SnapToGrid(cm.geom::geometry, :grid)
                ORDER BY COUNT(*) DESC
                LIMIT 500
                """;
        return jdbc.query(sql, p, (rs, __) -> new MapMediaItemDto(
                rs.getString("id"),
                "cluster",
                rs.getInt("count"),
                rs.getDouble("lat"),
                rs.getDouble("lng"),
                rs.getString("thumbnail_url"),
                null,
                null,
                null,
                null
        ));
    }

    private List<MapMediaItemDto> queryMedia(MapSqlParameterSource p) {
        final String sql = """
                SELECT
                  cm.id::text AS id,
                  cm.latitude AS lat,
                  cm.longitude AS lng,
                  COALESCE(cm.thumbnail_url, cm.media_url) AS thumbnail_url,
                  cm.media_url,
                  c.id::text AS collection_id,
                  c.title AS collection_name,
                  cm.captured_at
                FROM collection_media cm
                JOIN collections c ON c.id = cm.collection_id
                WHERE c.user_id = :userId
                  AND cm.latitude IS NOT NULL
                  AND cm.longitude IS NOT NULL
                  AND cm.geom IS NOT NULL
                  AND ST_Intersects(cm.geom::geometry, ST_MakeEnvelope(:minLng,:minLat,:maxLng,:maxLat,4326))
                  AND (:collectionId IS NULL OR c.id::text = :collectionId)
                  AND (:tripId IS NULL OR c.trip_id = :tripId)
                  AND (:mediaType IS NULL OR UPPER(cm.media_type) = UPPER(:mediaType))
                  AND (:recentFrom IS NULL OR cm.captured_at >= :recentFrom)
                ORDER BY cm.captured_at DESC NULLS LAST, cm.created_at DESC
                LIMIT 1200
                """;
        return jdbc.query(sql, p, (rs, __) -> new MapMediaItemDto(
                rs.getString("id"),
                "media",
                1,
                rs.getDouble("lat"),
                rs.getDouble("lng"),
                rs.getString("thumbnail_url"),
                rs.getString("media_url"),
                rs.getString("collection_id"),
                rs.getString("collection_name"),
                rs.getObject("captured_at", OffsetDateTime.class)
        ));
    }

    private static double[] parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return new double[]{-180d, -85d, 180d, 85d};
        }
        String[] p = bbox.split(",");
        if (p.length != 4) throw new IllegalArgumentException("bbox must be minLng,minLat,maxLng,maxLat");
        return new double[]{
                Double.parseDouble(p[0].trim()),
                Double.parseDouble(p[1].trim()),
                Double.parseDouble(p[2].trim()),
                Double.parseDouble(p[3].trim()),
        };
    }

    private static double clusterGrid(double zoom) {
        if (zoom <= 3) return 4.0;
        if (zoom <= 5) return 1.5;
        if (zoom <= 7) return 0.7;
        if (zoom <= 9) return 0.3;
        if (zoom <= 10.5) return 0.12;
        return 0.05;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static Timestamp recentFrom(String timeRange) {
        if (timeRange == null) return null;
        return switch (timeRange.trim().toUpperCase()) {
            case "LAST_7_DAYS" -> Timestamp.from(java.time.Instant.now().minus(java.time.Duration.ofDays(7)));
            default -> null;
        };
    }
}
