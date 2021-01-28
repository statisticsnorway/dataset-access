package no.ssb.useraccess.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private static String VARIABLE_PATTERN="$user";


    public static Optional<String> getDomain(String userId) {
        if (isValidEmail(userId)) {
            return Optional.of(userId.substring(userId.indexOf("@")+1));
        } else {
            throw new IllegalArgumentException(" Illegal user email address");
        }
    }

    public static Optional<String> getUserIdPart(String userId) {
        if (isValidEmail(userId)) {
            return Optional.of(userId.substring(0, userId.indexOf("@")));
        } else {
            throw new IllegalArgumentException(" Illegal user email address");
        }
    }

    public static boolean isValidEmail(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    public static String substitudeVariable(String stringToReplaceUserVariableIn, String userId) {
        return stringToReplaceUserVariableIn.replace(
                StringUtil.VARIABLE_PATTERN,
                userId
        );
    }
}
