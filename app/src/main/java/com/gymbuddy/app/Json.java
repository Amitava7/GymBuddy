package com.gymbuddy.app;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Writes JSON the way JavaScript's JSON.stringify(value, null, 2) does, so an
 * export from this app reads exactly like one from the React Native app.
 * Parsing goes through the framework's org.json.
 */
final class Json {

    private Json() {
    }

    static String stringify(Object value) {
        StringBuilder sb = new StringBuilder(1 << 16);
        write(sb, value, "");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object v, String indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            string(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Number) {
            sb.append(number((Number) v));
        } else if (v instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            String inner = indent + "  ";
            sb.append("{\n");
            boolean first = true;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!first) sb.append(",\n");
                first = false;
                sb.append(inner);
                string(sb, e.getKey());
                sb.append(": ");
                write(sb, e.getValue(), inner);
            }
            sb.append('\n').append(indent).append('}');
        } else if (v instanceof List) {
            List<Object> l = (List<Object>) v;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            String inner = indent + "  ";
            sb.append("[\n");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append(inner);
                write(sb, l.get(i), inner);
            }
            sb.append('\n').append(indent).append(']');
        } else {
            string(sb, v.toString());
        }
    }

    /** JavaScript's Number#toString for the values a database holds: 5, 12.5, 0.0005. */
    static String number(Number n) {
        if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
            return n.toString();
        }
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) return "null";
        if (d == 0) return "0";
        if (d == Math.rint(d) && Math.abs(d) < 1e21) return new BigDecimal(d).toPlainString();
        return new BigDecimal(Double.toString(d)).stripTrailingZeros().toPlainString();
    }

    private static void string(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    boolean lone = Character.isSurrogate(c) && !(Character.isHighSurrogate(c)
                            && i + 1 < s.length() && Character.isLowSurrogate(s.charAt(i + 1)))
                            && !(Character.isLowSurrogate(c) && i > 0
                            && Character.isHighSurrogate(s.charAt(i - 1)));
                    if (c < 0x20 || lone) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }
}
