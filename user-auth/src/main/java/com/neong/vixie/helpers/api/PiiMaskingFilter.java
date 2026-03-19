package com.neong.vixie.helpers.api;

/**
 * Utility for masking PII (Personally Identifiable Information) before logging.
 * <p>
 * NEVER log raw email, phone_number, or date_of_birth values.
 * Always use these masking methods when PII must appear in log context.
 */
public final class PiiMaskingFilter {

    private PiiMaskingFilter() {
        // Utility class — no instantiation
    }

    /**
     * Mask email: "john.doe@example.com" → "j***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@");
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    /**
     * Mask phone number: "+84912345678" → "***5678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    /**
     * Mask date of birth entirely: any value → "****-**-**"
     */
    public static String maskDob(String dob) {
        return "****-**-**";
    }
}
