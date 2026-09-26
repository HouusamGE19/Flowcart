package dev.houssam.flowcart.events;

import java.util.List;
import java.util.Map;

public final class EventData {
    private EventData() {
    }

    public static String string(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : value.toString();
    }

    public static long longValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static boolean bool(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> items(Map<String, Object> data) {
        Object value = data.get("items");
        if (value == null) {
            return List.of();
        }
        return (List<Map<String, Object>>) value;
    }
}
