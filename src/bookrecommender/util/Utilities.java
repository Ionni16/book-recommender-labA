package bookrecommender.util;

public class Utilities {

    public static boolean isStrongPassword(String pw) {
        if (pw == null || pw.length() < 8) return false;
        boolean hasLetter = false, hasDigit = false;
        for (char c : pw.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }
}
