package com.travelo.momentsservice.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristic timeline suggestions for moments AI (no ML). Scales cuts and highlight windows to clip duration.
 */
public final class MomentAiTimelinePlanner {

    private MomentAiTimelinePlanner() {
    }

    /**
     * Clamp client-supplied duration to a sane range (seconds).
     */
    public static double clampDurationSeconds(Double durationSec) {
        if (durationSec == null || durationSec.isNaN() || durationSec.isInfinite()) {
            return 60.0;
        }
        return Math.min(600.0, Math.max(4.0, durationSec));
    }

    /**
     * Scene-cut candidates (seconds from t=0), spread across the interior of the clip.
     */
    public static List<Double> buildSceneTimes(double durationSec) {
        double margin = Math.min(0.6, durationSec * 0.025);
        double inner = durationSec - 2 * margin;
        if (inner <= 0.5) {
            return List.of(round1(durationSec * 0.5));
        }
        int n = (int) Math.ceil(inner / 5.0);
        n = Math.min(Math.max(n, 4), 12);
        List<Double> out = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            out.add(round1(margin + inner * i / (n + 1)));
        }
        return out;
    }

    /**
     * Highlight windows for "best moments" UI (each has {@code highlight: true}).
     */
    public static List<Map<String, Object>> buildHighlights(double durationSec) {
        double w = Math.max(0.8, Math.min(durationSec * 0.17, durationSec / 3.5));
        List<Double> centers;
        if (durationSec < 7) {
            centers = List.of(durationSec * 0.33, durationSec * 0.72);
        } else if (durationSec < 22) {
            centers = List.of(durationSec * 0.22, durationSec * 0.52, durationSec * 0.78);
        } else {
            centers = List.of(durationSec * 0.18, durationSec * 0.42, durationSec * 0.63, durationSec * 0.86);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (double center : centers) {
            double start = round1(clamp(center - w / 2, 0, durationSec));
            double end = round1(clamp(center + w / 2, start + 0.5, durationSec));
            if (end - start < 0.55) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("start", start);
            row.put("end", end);
            row.put("highlight", true);
            out.add(row);
        }
        if (out.isEmpty() && durationSec >= 1.5) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("start", 0.0);
            row.put("end", round1(Math.min(durationSec, 2.5)));
            row.put("highlight", true);
            out.add(row);
        }
        return out;
    }

    /**
     * First {@code limit} highlight rows — used as suggested trim segments.
     */
    public static List<Map<String, Object>> firstSegments(List<Map<String, Object>> highlights, int limit) {
        return highlights.stream().limit(Math.max(0, limit)).map(MomentAiTimelinePlanner::copyRow).toList();
    }

    /**
     * Quantize segment boundaries for beat-style sync (0.25s grid).
     */
    public static List<Map<String, Object>> quantizeSegments(List<Map<String, Object>> segments, double durationSec) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : segments) {
            double start = toDouble(row.get("start"));
            double end = toDouble(row.get("end"));
            start = quantize(start, 0.25);
            end = quantize(end, 0.25);
            start = clamp(start, 0, durationSec);
            end = clamp(end, 0, durationSec);
            if (end <= start + 0.2) {
                end = Math.min(durationSec, start + 0.5);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start", round1(start));
            m.put("end", round1(end));
            m.put("highlight", Boolean.TRUE.equals(row.get("highlight")));
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> copyRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("start", row.get("start"));
        m.put("end", row.get("end"));
        m.put("highlight", row.get("highlight"));
        return m;
    }

    private static double quantize(double v, double step) {
        return Math.round(v / step) * step;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.min(hi, Math.max(lo, v));
    }

    private static double round1(double x) {
        return Math.round(x * 10.0) / 10.0;
    }
}
