import java.time.LocalDate;
import java.util.List;

/** Run with: java -ea Member4MatchServiceTest (no GUI required). */
public class Member4MatchServiceTest {
    private static int passed = 0;
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); passed++; }

    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 11, 13);
        Account student = new Account("Sara", "sara@example.com", "Student", "Information Technology", "Java, SQL, Git", "h");
        Account company = new Account("Acme", "hr@acme.com", "Company", "", "", "h");
        Store store = new Store();
        store.accounts.add(student); store.accounts.add(company);
        Opportunity full   = new Opportunity(1, company.email, "Java intern", "Internship", "Dubai", "Information Technology", "java, git", "2026-12-31", "d");
        Opportunity part   = new Opportunity(2, company.email, "Data intern", "Internship", "Dubai", "Statistics", "SQL, Python", "2026-12-01", "d");
        Opportunity noneReq= new Opportunity(3, company.email, "Office intern", "Internship", "Dubai", "Business", "", "2026-12-15", "d");
        Opportunity old    = new Opportunity(4, company.email, "Expired", "Internship", "Dubai", "IT", "java", "2026-01-01", "d");
        Opportunity closed = new Opportunity(5, company.email, "Closed", "Internship", "Dubai", "IT", "java", "2026-12-31", "d");
        closed.open = false;
        store.opportunities.addAll(List.of(full, part, noneReq, old, closed));

        check(Member4MatchService.parseSkills(" Java,, SQL ,java").size() == 2, "parse skills dedupes/trims");
        check(Member4MatchService.parseSkills(null).isEmpty(), "null skills");
        Member4MatchService.Match m1 = Member4MatchService.evaluate(student, full);
        check(m1.skillScore == 100 && m1.fieldMatches && m1.missingSkills.isEmpty(), "full match");
        Member4MatchService.Match m2 = Member4MatchService.evaluate(student, part);
        check(m2.skillScore == 50 && m2.missingSkills.contains("python"), "partial match");
        check(Member4MatchService.evaluate(student, noneReq).skillScore == 100, "no required skills");

        List<Member4MatchService.Match> recs = Member4MatchService.recommend(store, student, today, 0);
        check(recs.size() == 3, "excludes expired and closed");
        check(recs.get(0).opportunity == full, "best match first");
        check(recs.get(2).opportunity == part, "weakest last");
        check(Member4MatchService.recommend(store, student, today, 1).size() == 1, "limit respected");

        store.submitApplication(student, full);
        check(Member4MatchService.recommend(store, student, today, 0).size() == 2, "hides already-applied");
        try { Member4MatchService.recommend(store, company, today, 0); throw new AssertionError("company denied"); }
        catch (IllegalArgumentException expected) { passed++; }
        System.out.println("PASS: " + passed + " checks");
    }
}
