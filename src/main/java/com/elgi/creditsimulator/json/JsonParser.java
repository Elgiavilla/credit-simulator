package com.elgi.creditsimulator.json;

import com.elgi.creditsimulator.exception.RemoteServiceException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonParser {

    private final String source;
    private int position;

    private JsonParser(String source) {
        this.source = source;
    }

    public static Object parse(String json) {
        if (json == null) {
            throw new RemoteServiceException("The service returned no body at all.");
        }

        JsonParser parser = new JsonParser(json);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();

        if (!parser.atEnd()) {
            throw parser.failure("Unexpected trailing content after the JSON value");
        }
        return value;
    }

    /** Convenience for the common case: the response is expected to be an object. */
    public static JsonObject parseObject(String json) {
        Object value = parse(json);

        if (!(value instanceof Map)) {
            throw new RemoteServiceException(
                    "Expected the service to return a JSON object, but it returned "
                            + describe(value) + ".");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) value;
        return new JsonObject(fields);
    }

    // ------------------------------------------------------------ productions

    private Object readValue() {
        if (atEnd()) {
            throw failure("Expected a value but the input ended");
        }

        char c = peek();
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                readLiteral("true");
                return Boolean.TRUE;
            case 'f':
                readLiteral("false");
                return Boolean.FALSE;
            case 'n':
                readLiteral("null");
                return null;
            default:
                if (c == '-' || isDigit(c)) {
                    return readNumber();
                }
                throw failure("Unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> fields = new LinkedHashMap<>();

        skipWhitespace();
        if (peekIs('}')) {
            position++;
            return fields;
        }

        while (true) {
            skipWhitespace();
            String key = readString();

            skipWhitespace();
            expect(':');

            skipWhitespace();
            fields.put(key, readValue());

            skipWhitespace();
            char c = next("Expected ',' or '}' in object");
            if (c == '}') {
                return fields;
            }
            if (c != ',') {
                throw failure("Expected ',' or '}' in object but found '" + c + "'");
            }
        }
    }

    private List<Object> readArray() {
        expect('[');
        List<Object> items = new ArrayList<>();

        skipWhitespace();
        if (peekIs(']')) {
            position++;
            return items;
        }

        while (true) {
            skipWhitespace();
            items.add(readValue());

            skipWhitespace();
            char c = next("Expected ',' or ']' in array");
            if (c == ']') {
                return items;
            }
            if (c != ',') {
                throw failure("Expected ',' or ']' in array but found '" + c + "'");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder text = new StringBuilder();

        while (true) {
            char c = next("Unterminated string");

            if (c == '"') {
                return text.toString();
            }
            if (c != '\\') {
                if (c < 0x20) {
                    throw failure("Unescaped control character in string");
                }
                text.append(c);
                continue;
            }

            char escape = next("Unterminated escape sequence");
            switch (escape) {
                case '"':  text.append('"');  break;
                case '\\': text.append('\\'); break;
                case '/':  text.append('/');  break;
                case 'b':  text.append('\b'); break;
                case 'f':  text.append('\f'); break;
                case 'n':  text.append('\n'); break;
                case 'r':  text.append('\r'); break;
                case 't':  text.append('\t'); break;
                case 'u':  text.append(readUnicodeEscape()); break;
                default:
                    throw failure("Unknown escape sequence '\\" + escape + "'");
            }
        }
    }

    private char readUnicodeEscape() {
        if (position + 4 > source.length()) {
            throw failure("Truncated \\u escape");
        }
        String hex = source.substring(position, position + 4);
        try {
            char value = (char) Integer.parseInt(hex, 16);
            position += 4;
            return value;
        } catch (NumberFormatException cause) {
            throw failure("Invalid \\u escape '" + hex + "'");
        }
    }


    private BigDecimal readNumber() {
        int start = position;

        if (peekIs('-')) {
            position++;
        }
        readIntegerPart();

        if (peekIs('.')) {
            position++;
            readDigits();
        }
        if (peekIs('e') || peekIs('E')) {
            position++;
            if (peekIs('+') || peekIs('-')) {
                position++;
            }
            readDigits();
        }

        String number = source.substring(start, position);
        try {
            return new BigDecimal(number);
        } catch (NumberFormatException cause) {
            throw failure("Invalid number '" + number + "'");
        }
    }

    private void readIntegerPart() {
        if (atEnd() || !isDigit(peek())) {
            throw failure("Expected a digit");
        }

        if (peek() == '0') {
            position++;
            if (!atEnd() && isDigit(peek())) {
                throw failure("Leading zeros are not allowed in JSON numbers");
            }
            return;
        }
        readDigits();
    }

    private void readDigits() {
        int start = position;
        while (!atEnd() && isDigit(peek())) {
            position++;
        }
        if (position == start) {
            throw failure("Expected a digit");
        }
    }

    private void readLiteral(String literal) {
        if (!source.startsWith(literal, position)) {
            throw failure("Expected '" + literal + "'");
        }
        position += literal.length();
    }

    private void skipWhitespace() {
        while (!atEnd() && isWhitespace(peek())) {
            position++;
        }
    }

    private boolean atEnd() {
        return position >= source.length();
    }

    private char peek() {
        return source.charAt(position);
    }

    private boolean peekIs(char expected) {
        return !atEnd() && peek() == expected;
    }

    private char next(String messageIfEnded) {
        if (atEnd()) {
            throw failure(messageIfEnded);
        }
        return source.charAt(position++);
    }

    private void expect(char expected) {
        char c = next("Expected '" + expected + "' but the input ended");
        if (c != expected) {
            throw failure("Expected '" + expected + "' but found '" + c + "'");
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private RemoteServiceException failure(String message) {
        return new RemoteServiceException(
                "Malformed JSON at position " + position + ": " + message + ".");
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List) {
            return "an array";
        }
        if (value instanceof String) {
            return "a string";
        }
        if (value instanceof BigDecimal) {
            return "a number";
        }
        return "a " + value.getClass().getSimpleName();
    }

}
