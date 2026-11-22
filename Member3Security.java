import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Member 3 contribution: input validation and role-based authorization.
 * Keeps security rules in one place instead of scattering them through Swing screens.
 */
public final class Member3Security {
    private Member3Security() { }

    public static boolean validEmail(String email) {
        return email != null && email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public static boolean validRegistration(String name, String email, String password) {
        return name != null && !name.isBlank() && validEmail(email)
                && password != null && password.length() >= 6;
    }

    public static boolean validOpportunity(String title, String location, String field,
                                           String description, String deadline) {
        if (blank(title) || blank(location) || blank(field) || blank(description) || blank(deadline))
            return false;
        try {
            LocalDate.parse(deadline); // ISO yyyy-MM-dd, rejects invalid calendar dates
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    public static boolean isApplicant(Account account) {
        return account != null && ("Student".equals(account.role) || "Graduate".equals(account.role));
    }

    public static boolean isCompany(Account account) {
        return account != null && "Company".equals(account.role);
    }

    public static boolean ownsOpportunity(Account company, Opportunity opportunity) {
        return isCompany(company) && opportunity != null
                && company.email != null && opportunity.companyEmail != null
                && company.email.trim().toLowerCase(Locale.ROOT)
                   .equals(opportunity.companyEmail.trim().toLowerCase(Locale.ROOT));
    }

    /** The store must hold the exact listing; callers may not close a fabricated object. */
    public static void closeOpportunity(Store store, Account company, Opportunity opportunity) {
        if (store == null || opportunity == null || store.opportunity(opportunity.id) != opportunity
                || !ownsOpportunity(company, opportunity)) {
            throw new IllegalArgumentException("Only the posting company can close this opportunity.");
        }
        opportunity.open = false;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
