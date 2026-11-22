import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Member 4 contribution: opportunity recommendation ("matching") service.
 * Compares an applicant's skills and major with each open opportunity and ranks
 * the results, so a qualified student sees the best-fitting internships first.
 * Pure logic, no GUI: it only reads from Store and never modifies it.
 */
public final class Member4MatchService {
    private Member4MatchService() { }

    /** Immutable result of comparing one applicant with one opportunity. */
    public static final class Match {
        public final Opportunity opportunity;
        public final int skillScore;              // 0-100: % of required skills the applicant has
        public final boolean fieldMatches;        // major and opportunity field overlap
        public final List<String> matchedSkills;
        public final List<String> missingSkills;

        Match(Opportunity o, int score, boolean field, List<String> matched, List<String> missing) {
            this.opportunity = o; this.skillScore = score; this.fieldMatches = field;
            this.matchedSkills = Collections.unmodifiableList(matched);
            this.missingSkills = Collections.unmodifiableList(missing);
        }
    }

    /** Splits a comma-separated skills string into lower-case, trimmed, distinct skills. */
    public static Set<String> parseSkills(String csv) {
        Set<String> skills = new LinkedHashSet<>();
        if (csv == null) return skills;
        for (String s : csv.split(",")) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) skills.add(t);
        }
        return skills;
    }

    /** Compares one applicant with one opportunity. */
    public static Match evaluate(Account applicant, Opportunity opportunity) {
        if (applicant == null || opportunity == null)
            throw new IllegalArgumentException("Applicant and opportunity are required.");
        Set<String> have = parseSkills(applicant.skills);
        Set<String> need = parseSkills(opportunity.skills);
        List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
        for (String s : need) (have.contains(s) ? matched : missing).add(s);
        int score = need.isEmpty() ? 100 : (int) Math.round(100.0 * matched.size() / need.size());
        return new Match(opportunity, score, fieldOverlaps(applicant.major, opportunity.field), matched, missing);
    }

    /**
     * Ranked recommendations for an applicant: open, not-expired opportunities the
     * applicant has not applied to yet. Best skill score first, then field match,
     * then the earliest deadline. Only students/graduates may ask for them.
     */
    public static List<Match> recommend(Store store, Account applicant, LocalDate today, int limit) {
        if (store == null || !Member3Security.isApplicant(applicant))
            throw new IllegalArgumentException("Only students and graduates get recommendations.");
        List<Match> result = new ArrayList<>();
        for (Opportunity o : store.opportunities) {
            if (!o.open || expired(o, today)) continue;
            if (store.application(applicant.email, o.id) != null) continue;
            result.add(evaluate(applicant, o));
        }
        result.sort(Comparator
                .comparingInt((Match m) -> -m.skillScore)
                .thenComparing(m -> !m.fieldMatches)
                .thenComparing(m -> m.opportunity.deadline));
        return limit > 0 && result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    private static boolean expired(Opportunity o, LocalDate today) {
        try { return LocalDate.parse(o.deadline).isBefore(today); }
        catch (DateTimeParseException | NullPointerException e) { return true; }
    }

    private static boolean fieldOverlaps(String major, String field) {
        String a = major == null ? "" : major.trim().toLowerCase(Locale.ROOT);
        String b = field == null ? "" : field.trim().toLowerCase(Locale.ROOT);
        return !a.isEmpty() && !b.isEmpty() && (a.contains(b) || b.contains(a));
    }
}
