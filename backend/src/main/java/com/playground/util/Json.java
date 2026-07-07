package com.playground.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny zero-dependency JSON encoder/decoder.
 *
 * <p>Supported types: {@code null}, {@link Boolean}, {@link Number}, {@link String},
 * {@link Map} (with String keys), {@link List}, primitive int / double arrays
 * and 2-D double arrays. The decoder follows RFC 7159 closely enough for the
 * payload shapes used by the playground REST API.
 */
public final class Json {

    private Json() {}

    // ------------------------------------------------------------------
    //  Encoder
    // ------------------------------------------------------------------

    public static String encode(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) { sb.append("null"); return; }
        if (v instanceof Boolean) { sb.append(((Boolean) v) ? "true" : "false"); return; }
        if (v instanceof Number) { writeNumber(sb, ((Number) v).doubleValue(), v); return; }
        if (v instanceof String) { writeString(sb, (String) v); return; }
        if (v instanceof Map) { writeObject(sb, (Map<?, ?>) v); return; }
        if (v instanceof List) { writeArray(sb, (List<?>) v); return; }
        if (v instanceof double[]) { writeDoubleArray(sb, (double[]) v); return; }
        if (v instanceof double[][]) { writeDoubleMatrix(sb, (double[][]) v); return; }
        if (v instanceof int[]) { writeIntArray(sb, (int[]) v); return; }
        if (v instanceof Object[]) { writeObjectArray(sb, (Object[]) v); return; }
        throw new IllegalArgumentException("Unsupported JSON type: " + v.getClass());
    }

    private static void writeNumber(StringBuilder sb, double d, Object original) {
        if (Double.isNaN(d) || Double.isInfinite(d)) { sb.append("null"); return; }
        if (original instanceof Integer || original instanceof Long || original instanceof Short || original instanceof Byte) {
            sb.append(((Number) original).longValue());
            return;
        }
        if (d == (long) d && Math.abs(d) < 1e15) {
            sb.append((long) d);
        } else {
            sb.append(d);
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, list.get(i));
        }
        sb.append(']');
    }

    private static void writeObjectArray(StringBuilder sb, Object[] arr) {
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, arr[i]);
        }
        sb.append(']');
    }

    private static void writeDoubleArray(StringBuilder sb, double[] arr) {
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            writeNumber(sb, arr[i], arr[i]);
        }
        sb.append(']');
    }

    private static void writeDoubleMatrix(StringBuilder sb, double[][] mat) {
        sb.append('[');
        for (int i = 0; i < mat.length; i++) {
            if (i > 0) sb.append(',');
            writeDoubleArray(sb, mat[i]);
        }
        sb.append(']');
    }

    private static void writeIntArray(StringBuilder sb, int[] arr) {
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        sb.append(']');
    }

    // ------------------------------------------------------------------
    //  Decoder
    // ------------------------------------------------------------------

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object v = p.parseValue();
        p.skipWs();
        if (p.pos != p.text.length()) {
            throw new IllegalArgumentException("Trailing data at position " + p.pos);
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (v instanceof Map) return (Map<String, Object>) v;
        throw new IllegalArgumentException("Expected JSON object");
    }

    private static final class Parser {
        final String text;
        int pos;

        Parser(String text) { this.text = text; }

        void skipWs() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        Object parseValue() {
            skipWs();
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            char c = text.charAt(pos);
            if (c == '{') return parseObjectInternal();
            if (c == '[') return parseArrayInternal();
            if (c == '"') return parseStringInternal();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumberInternal();
        }

        Map<String, Object> parseObjectInternal() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWs();
            if (peek() == '}') { pos++; return map; }
            while (true) {
                skipWs();
                String key = parseStringInternal();
                skipWs();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; return map; }
                throw new IllegalArgumentException("Expected ',' or '}' at position " + pos);
            }
        }

        List<Object> parseArrayInternal() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWs();
            if (peek() == ']') { pos++; return list; }
            while (true) {
                list.add(parseValue());
                skipWs();
                char c = peek();
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; return list; }
                throw new IllegalArgumentException("Expected ',' or ']' at position " + pos);
            }
        }

        String parseStringInternal() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end of escape");
                    char esc = text.charAt(pos++);
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > text.length()) throw new IllegalArgumentException("Invalid unicode escape");
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                            pos += 4;
                            break;
                        default: throw new IllegalArgumentException("Invalid escape: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        Boolean parseBoolean() {
            if (text.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
            if (text.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (text.startsWith("null", pos)) { pos += 4; return null; }
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }

        Number parseNumberInternal() {
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < text.length() && "0123456789.eE+-".indexOf(text.charAt(pos)) >= 0) pos++;
            String token = text.substring(start, pos);
            try {
                if (token.contains(".") || token.contains("e") || token.contains("E")) {
                    return Double.parseDouble(token);
                }
                long l = Long.parseLong(token);
                if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE) return (int) l;
                return l;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + token, ex);
            }
        }

        char peek() {
            if (pos >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return text.charAt(pos);
        }

        void expect(char c) {
            if (pos >= text.length() || text.charAt(pos) != c) {
                throw new IllegalArgumentException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }
    }

    // ------------------------------------------------------------------
    //  Type helpers
    // ------------------------------------------------------------------

    public static double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return defaultValue;
    }

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return defaultValue;
    }

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object v = map.get(key);
        if (v instanceof String) return (String) v;
        return defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List) return (List<Object>) v;
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Map) return (Map<String, Object>) v;
        return null;
    }

    public static List<String> toStringList(List<Object> list) {
        if (list == null) return null;
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) out.add(String.valueOf(o));
        return out;
    }

    public static int[] toIntArray(List<Object> list) {
        if (list == null) return new int[0];
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof Number) out[i] = ((Number) o).intValue();
            else throw new IllegalArgumentException("Expected number, got " + o);
        }
        return out;
    }
}
