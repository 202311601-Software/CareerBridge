/** Run with: java -ea Member3SecurityTest (no GUI required). */
public class Member3SecurityTest {
    private static int passed=0;
    private static void check(boolean okay,String name){if(!okay)throw new AssertionError(name);passed++;}
    private static void denied(Runnable action,String name){
        try {action.run();throw new AssertionError("Should be denied: "+name);}
        catch(IllegalArgumentException expected){passed++;}
    }
    public static void main(String[] args){
        check(Member3Security.validRegistration("Student","test@example.com","abcdef"),"valid registration");
        check(!Member3Security.validRegistration("","test@example.com","abcdef"),"empty name");
        check(!Member3Security.validRegistration("Student","invalid-email","abcdef"),"invalid email");
        check(!Member3Security.validRegistration("Student","test@example.com","short"),"short password");
        check(Member3Security.validOpportunity("Java intern","Dubai","IT","Learn Java","2026-12-31"),"valid posting");
        check(!Member3Security.validOpportunity("Java intern","Dubai","IT","Learn Java","2026-02-30"),"bad date");
        check(!Member3Security.validOpportunity(" ","Dubai","IT","Learn Java","2026-12-31"),"blank title");
        Account student=new Account("Student","student@example.com","Student","IT","Java","hash");
        Account company=new Account("Company A","a@example.com","Company","","","hash");
        Account other=new Account("Company B","b@example.com","Company","","","hash");
        Store store=new Store();
        store.accounts.add(student);store.accounts.add(company);store.accounts.add(other);
        Opportunity listing=new Opportunity(1,company.email,"Java intern","Internship","Dubai","IT","Java","2026-12-31","Description");
        store.opportunities.add(listing);
        check(Member3Security.isApplicant(student),"applicant role");
        check(!Member3Security.isApplicant(company),"company not applicant");
        check(Member3Security.ownsOpportunity(company,listing),"own listing");
        check(!Member3Security.ownsOpportunity(other,listing),"not own listing");
        denied(()->Member3Security.closeOpportunity(store,other,listing),"other company cannot close");
        check(listing.open,"still open after denial");
        denied(()->Member3Security.closeOpportunity(store,student,listing),"student cannot close");
        denied(()->Member3Security.closeOpportunity(store,company,new Opportunity(1,company.email,"Fake","Internship","Dubai","IT","Java","2026-12-31","Fake")),"fake listing cannot close");
        Application a=store.submitApplication(student,listing);
        check("Pending".equals(a.status),"pending at submission");
        denied(()->store.submitApplication(student,listing),"duplicate application");
        denied(()->store.updateApplicationStatus(other,a,"Accepted"),"other company cannot approve");
        store.updateApplicationStatus(company,a,"Under Review");
        check("Under Review".equals(a.status),"company can update");
        Member3Security.closeOpportunity(store,company,listing);
        check(!listing.open,"owner closes listing");
        denied(()->store.submitApplication(student,listing),"cannot apply to closed listing");
        System.out.println("PASS: "+passed+" checks");
    }
}
