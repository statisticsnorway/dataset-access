package no.ssb.useraccess.token;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import no.ssb.useraccess.token.Token.TokenParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TokenTest {

    @Test
    void shouldParseCorrectly() throws Exception {
        /*
          {
           "typ": "JWT",
           "alg": "HS256"
          }
          .
          {
           "sub": "1234567890",
           "name": "Stan Statistics",
           "admin": true,
           "jti": "645b5a28-392b-4b87-a3a6-41eebd7a0ed8",
           "iat": 1574328348,
           "exp": 1574331948
          }
          .
          secret
         */
        final String authorization = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlN0YW4gU3RhdGlzdGljcyIsImFkbWluIjp0cnVlLCJqdGkiOiI2NDViNWEyOC0zOTJiLTRiODctYTNhNi00MWVlYmQ3YTBlZDgiLCJpYXQiOjE1NzQzMjgzNDgsImV4cCI6MTU3NDMzMjAwMX0.xY91f_Y2U8XRs_42cipoEHRuJ80UON5lzBv5ytZlLow";

        final Token token = Token.create(authorization);

        assertThat(token.getUserId()).isEqualTo("1234567890");
    }

    @Test
    void shouldFailWhenSubIsMissing() {

        /*
          {
           "typ": "JWT",
           "alg": "HS256"
          }
          .
          {
           "name": "Stan Statistics",
           "admin": true,
           "jti": "645b5a28-392b-4b87-a3a6-41eebd7a0ed8",
           "iat": 1574328348,
           "exp": 1574331948
          }
          .
          secret
         */
        final String authorization = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiU3RhbiBTdGF0aXN0aWNzIiwiYWRtaW4iOnRydWUsImp0aSI6IjY0NWI1YTI4LTM5MmItNGI4Ny1hM2E2LTQxZWViZDdhMGVkOCIsImlhdCI6MTU3NDMyODM0OCwiZXhwIjoxNTc0MzMyNTk4fQ.EVwM3OraRU05GKSCZyPc1tSBP-Pte-K_5NwLCxiC7gI";

        assertThatExceptionOfType(TokenParseException.class)
                .isThrownBy(() -> Token.create(authorization))
                .withMessage("Failed to parse request token")
                .withCauseInstanceOf(MismatchedInputException.class);
    }

    @Test
    void shouldFailWhenBearerIsMissing() {

        /*
          {
           "typ": "JWT",
           "alg": "HS256"
          }
          .
          {
           "name": "Stan Statistics",
           "admin": true,
           "jti": "645b5a28-392b-4b87-a3a6-41eebd7a0ed8",
           "iat": 1574328348,
           "exp": 1574331948
          }
          .
          secret
         */
        final String authorization = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYW1lIjoiU3RhbiBTdGF0aXN0aWNzIiwiYWRtaW4iOnRydWUsImp0aSI6IjY0NWI1YTI4LTM5MmItNGI4Ny1hM2E2LTQxZWViZDdhMGVkOCIsImlhdCI6MTU3NDMyODM0OCwiZXhwIjoxNTc0MzMyNTk4fQ.EVwM3OraRU05GKSCZyPc1tSBP-Pte-K_5NwLCxiC7gI";

        assertThatExceptionOfType(TokenParseException.class)
                .isThrownBy(() -> Token.create(authorization))
                .withMessage("Invalid token. Expected \"Bearer <token>\"")
                .withNoCause();
    }

    @Test
    void shouldFailWhenTokenIsNull() {
        final String authorization = null;

        assertThatExceptionOfType(TokenParseException.class)
                .isThrownBy(() -> Token.create(authorization))
                .withMessage("Invalid token. Expected \"Bearer <token>\"")
                .withNoCause();
    }

    @Test
    void shouldFailWhenTokenIsEmpty() {
        final String authorization = "";

        assertThatExceptionOfType(TokenParseException.class)
                .isThrownBy(() -> Token.create(authorization))
                .withMessage("Invalid token. Expected \"Bearer <token>\"")
                .withNoCause();
    }
}
