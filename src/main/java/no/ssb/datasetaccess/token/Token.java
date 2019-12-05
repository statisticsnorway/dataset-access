package no.ssb.datasetaccess.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Token {

    private final String userId;

    String getUserId() {
        return userId;
    }

    @JsonCreator
    public Token(@JsonProperty(value = "sub", required = true) String userId) {
        this.userId = userId;
    }

    static Token create(final String authorization) throws TokenParseException {

        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            throw new TokenParseException("Invalid token. Expected \"Bearer <token>\"");
        }

        final Token token;

        try {

            final String[] parts = authorization.split("\\.");

            final String payload = parts[1];

            final String decodedPayload = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);

            token = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(decodedPayload, Token.class);

        } catch (Exception e) {
            throw new TokenParseException("Failed to parse request token", e);
        }

        return token;
    }

    static class TokenParseException extends Exception {
        TokenParseException(String message) {
            super(message);
        }

        TokenParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
