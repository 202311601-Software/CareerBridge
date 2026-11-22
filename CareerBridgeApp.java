import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.*;
import java.util.List;

/** CareerBridge: a small educational Java Swing internship/graduate-job prototype. */
public class CareerBridgeApp extends JFrame {
    private final Store store = Store.load();
    private Account current;
    private final JPanel root = new JPanel(new BorderLayout());
    private final Color blue = new Color(28, 65, 112);

    public CareerBridgeApp() {
        super("CareerBridge | Internships & Graduate Jobs");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 680); setLocationRelativeTo(null);
        setContentPane(root); showWelcome();
    }
    private JButton button(String text, Runnable task) { JButton b = new JButton(text); b.addActionListener(e -> task.run()); return b; }
    private JPanel line(Component... cs) { JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)); for(Component c:cs)p.add(c);return p; }
    private JLabel title(String text) { JLabel l=new JLabel(text); l.setFont(new Font("SansSerif",Font.BOLD,22));l.setForeground(blue);return l; }
    private void page(String heading, JPanel content) {
        root.removeAll(); JPanel top=new JPanel(new BorderLayout()); top.setBorder(BorderFactory.createEmptyBorder(16,20,12,20));
        top.add(title(heading),BorderLayout.WEST); root.add(top,BorderLayout.NORTH);
        JScrollPane sc=new JScrollPane(content);sc.setBorder(null); root.add(sc,BorderLayout.CENTER);
        root.revalidate();root.repaint();
    }
    private JPanel stack() { JPanel p=new JPanel();p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(12,18,18,18));return p; }
    private void showWelcome() {
        JPanel p=stack();p.add(new JLabel("Find internships and entry-level jobs without prior experience."));
        p.add(line(button("Log in",this::login),button("Create account",this::register)));
        p.add(new JLabel("Demo: register an applicant or company. Company posts opportunities; applicant applies."));
        page("Welcome to CareerBridge",p);
    }
    private String ask(String field) { return JOptionPane.showInputDialog(this,field); }
    private void register() {
        String[] roles={"Student","Graduate","Company"};
        String role=(String)JOptionPane.showInputDialog(this,"Choose account type","Register",JOptionPane.PLAIN_MESSAGE,null,roles,roles[0]);
        if(role==null)return;
        String name=ask(role.equals("Company")?"Company name":"Full name"); if(name==null)return;
        String email=ask("Email"); if(email==null)return;
        JPasswordField pass=new JPasswordField(); if(JOptionPane.showConfirmDialog(this,pass,"Password (at least 6 characters)",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String pw=new String(pass.getPassword());
        if(!Member3Security.validRegistration(name,email,pw)) {error("Enter a name, valid email, and password of 6+ characters.");return;}
        if(store.account(email)!=null){error("Email already registered.");return;}
        String major="",skills="";
        if(!role.equals("Company")){major=ask("University major / field of study"); if(major==null)return;skills=ask("Skills (comma separated)");if(skills==null)return;}
        Account a=new Account(name.trim(),email.trim().toLowerCase(),role,major.trim(),skills.trim(),Passwords.hash(pw));
        store.accounts.add(a);store.save();current=a;dashboard();
    }
    private void login() {
        String email=ask("Email");if(email==null)return;
        JPasswordField pass=new JPasswordField();if(JOptionPane.showConfirmDialog(this,pass,"Password",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        Account a=store.account(email.trim());
        if(a==null || !Passwords.verify(new String(pass.getPassword()),a.passwordHash)){error("Invalid email or password.");return;}
        current=a;dashboard();
    }
    private void logout(){current=null;showWelcome();}
    private void dashboard(){if(current==null){showWelcome();return;} if(current.role.equals("Company"))companyView();else applicantView();}
    private void applicantView(){
        JPanel p=stack();p.add(new JLabel("Signed in: "+current.name+" ("+current.role+")"));
        p.add(line(button("Edit profile",this::editProfile),button("My applications",this::myApplications),button("Recommended for me",this::recommended),button("Log out",this::logout)));
        JTextField search=new JTextField(18),loc=new JTextField(13),field=new JTextField(13);
        JComboBox<String> type=new JComboBox<>(new String[]{"All","Internship","Entry-Level Job"});
        p.add(line(new JLabel("Keyword"),search,new JLabel("Location"),loc));
        p.add(line(new JLabel("Field"),field,new JLabel("Type"),type));
        String[] cols={"ID","Title","Company","Type","Location","Field","Deadline"};
        DefaultTableModel model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        JTable table=new JTable(model); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);p.add(new JScrollPane(table));
        Runnable refresh=()->{
            model.setRowCount(0);
            for (Opportunity o : store.search(search.getText(), loc.getText(), field.getText(), (String)type.getSelectedItem())) {
                model.addRow(new Object[]{o.id, o.title, store.companyName(o.companyEmail), o.type, o.location, o.field, o.deadline});
            }
        };refresh.run();
        p.add(line(button("Search / Filter",refresh),button("View selected details",()->{
            Opportunity o=selected(table,model);if(o!=null)JOptionPane.showMessageDialog(this,o.title+"\nCompany: "+store.companyName(o.companyEmail)+"\nType: "+o.type+"\nLocation: "+o.location+"\nField: "+o.field+"\nSkills: "+o.skills+"\nDeadline: "+o.deadline+"\nDescription: "+o.description);
        }),button("Apply to selected",()->{
            Opportunity o=selected(table,model);if(o==null)return;
            try {
                store.submitApplication(current, o);
                store.save();
                JOptionPane.showMessageDialog(this, "Application submitted (Pending).");
            } catch (IllegalArgumentException ex) { error(ex.getMessage()); }
        })));
        page("Browse opportunities",p);
    }
    private void recommended(){
        StringBuilder sb=new StringBuilder();
        for(Member4MatchService.Match m:Member4MatchService.recommend(store,current,java.time.LocalDate.now(),5))
            sb.append(m.opportunity.title).append(" (").append(store.companyName(m.opportunity.companyEmail)).append(") - ")
              .append(m.skillScore).append("% skill match")
              .append(m.missingSkills.isEmpty()?"":", missing: "+String.join(", ",m.missingSkills)).append("\n");
        JOptionPane.showMessageDialog(this,sb.length()==0?"No recommendations yet. Add skills to your profile or check back later.":sb.toString(),"Recommended for you",JOptionPane.INFORMATION_MESSAGE);
    }
    private Opportunity selected(JTable table,DefaultTableModel model){int row=table.getSelectedRow();if(row<0){error("Select an opportunity first.");return null;} int id=(int)model.getValueAt(table.convertRowIndexToModel(row),0);return store.opportunity(id);}
    private void editProfile(){
        String major=ask("Major / field (current: "+current.major+")");if(major==null)return;
        String skills=ask("Skills (current: "+current.skills+")");if(skills==null)return;
        current.major=major.trim();current.skills=skills.trim();store.save();dashboard();
    }
    private void myApplications(){
        JPanel p=stack();p.add(line(button("Back",this::dashboard),button("Log out",this::logout)));
        DefaultTableModel m=new DefaultTableModel(new String[]{"Opportunity","Company","Status"},0);
        for(Application a:store.applications)if(a.applicantEmail.equals(current.email)){
            Opportunity o=store.opportunity(a.opportunityId);if(o!=null)m.addRow(new Object[]{o.title,store.companyName(o.companyEmail),a.status});
        }p.add(new JScrollPane(new JTable(m)));page("My applications",p);
    }
    private void companyView(){
        JPanel p=stack();p.add(new JLabel("Company account: "+current.name));
        p.add(line(button("Post opportunity",this::post),button("Review applications",this::reviewApplications),button("Log out",this::logout)));
        DefaultTableModel m=new DefaultTableModel(new String[]{"ID","Title","Type","Location","Open"},0){public boolean isCellEditable(int r,int c){return false;}};
        for(Opportunity o:store.opportunities)if(o.companyEmail.equals(current.email))m.addRow(new Object[]{o.id,o.title,o.type,o.location,o.open});
        JTable t=new JTable(m);p.add(new JScrollPane(t));
        p.add(line(button("Close selected opportunity",()->{int r=t.getSelectedRow();if(r<0){error("Select an opportunity.");return;} Opportunity o=store.opportunity((int)m.getValueAt(r,0));if(o!=null){try{Member3Security.closeOpportunity(store,current,o);store.save();companyView();}catch(IllegalArgumentException ex){error(ex.getMessage());}}})));
        page("Company dashboard",p);
    }
    private void post(){
        String title=ask("Opportunity title");if(title==null)return;
        String[] kinds={"Internship","Entry-Level Job"};String type=(String)JOptionPane.showInputDialog(this,"Type","Post",JOptionPane.PLAIN_MESSAGE,null,kinds,kinds[0]);if(type==null)return;
        String location=ask("Location");if(location==null)return;
        String field=ask("Field of study");if(field==null)return;
        String skills=ask("Required skills");if(skills==null)return;
        String deadline=ask("Deadline (YYYY-MM-DD)");if(deadline==null)return;
        String desc=ask("Description");if(desc==null)return;
        if(!Member3Security.isCompany(current)){error("Only companies can publish opportunities.");return;}
        if(!Member3Security.validOpportunity(title,location,field,desc,deadline)){error("Fill required fields and use a valid YYYY-MM-DD date.");return;}
        int id=store.nextId++;store.opportunities.add(new Opportunity(id,current.email,title,type,location,field,skills,deadline,desc));store.save();companyView();
    }
    private void reviewApplications(){
        JPanel p=stack();p.add(line(button("Back",this::dashboard)));
        DefaultTableModel m=new DefaultTableModel(new String[]{"Opportunity ID","Opportunity","Applicant","Major","Skills","Status"},0){public boolean isCellEditable(int r,int c){return false;}};
        List<Application> rows=new ArrayList<>();
        for(Application a:store.applications){Opportunity o=store.opportunity(a.opportunityId);if(o==null||!o.companyEmail.equals(current.email))continue;Account acc=store.account(a.applicantEmail);if(acc==null)continue;rows.add(a);m.addRow(new Object[]{o.id,o.title,acc.name+" ("+acc.email+")",acc.major,acc.skills,a.status});}
        JTable table=new JTable(m);p.add(new JScrollPane(table));
        p.add(line(button("Mark Under Review",()->changeStatus(table,rows,"Under Review")),button("Accept",()->changeStatus(table,rows,"Accepted")),button("Reject",()->changeStatus(table,rows,"Rejected"))));
        page("Review applications",p);
    }
    private void changeStatus(JTable table,List<Application> rows,String newStatus){int r=table.getSelectedRow();if(r<0){error("Select an application.");return;}Application a=rows.get(table.convertRowIndexToModel(r));
        try { store.updateApplicationStatus(current, a, newStatus); store.save(); reviewApplications(); }
        catch (IllegalArgumentException ex) { error(ex.getMessage()); }}
    private void error(String s){JOptionPane.showMessageDialog(this,s,"CareerBridge",JOptionPane.ERROR_MESSAGE);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new CareerBridgeApp().setVisible(true));}
}
class Account implements Serializable {
    private static final long serialVersionUID=1L;
    String name,email,role,major,skills,passwordHash;
    Account(String n,String e,String r,String m,String s,String h){name=n;email=e;role=r;major=m;skills=s;passwordHash=h;}
}
class Opportunity implements Serializable {
    private static final long serialVersionUID=1L;
    int id;String companyEmail,title,type,location,field,skills,deadline,description;boolean open=true;
    Opportunity(int id,String c,String t,String ty,String l,String f,String s,String d,String desc){this.id=id;companyEmail=c;title=t;type=ty;location=l;field=f;skills=s;deadline=d;description=desc;}
}
class Application implements Serializable {
    private static final long serialVersionUID=1L;
    String applicantEmail,status="Pending";int opportunityId;
    Application(String a,int id){applicantEmail=a;opportunityId=id;}
}
class Store implements Serializable {
    private static final long serialVersionUID = 1L;
    List<Account> accounts = new ArrayList<>();
    List<Opportunity> opportunities = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    int nextId = 1;

    static Path path() { return Paths.get(System.getProperty("user.home"), ".careerbridge", "data.bin"); }

    static Store load() {
        if (!Files.exists(path())) return new Store();
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path()))) {
            return (Store) in.readObject();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not read saved CareerBridge data. The existing file was not overwritten.\n" + e.getMessage(),
                    "CareerBridge: Data error", JOptionPane.ERROR_MESSAGE);
            throw new IllegalStateException("Cannot load local data", e);
        }
    }

    void save() {
        Path destination = path();
        Path temporary = destination.resolveSibling("data.tmp");
        try {
            Files.createDirectories(destination.getParent());
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                out.writeObject(this);
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) { throw new IllegalStateException("Could not save local data", e); }
    }

    Account account(String email) {
        if (email == null) return null;
        for (Account a : accounts) if (a.email.equalsIgnoreCase(email.trim())) return a;
        return null;
    }
    Opportunity opportunity(int id) {
        for (Opportunity o : opportunities) if (o.id == id) return o;
        return null;
    }
    Application application(String applicant, int opportunityId) {
        for (Application a : applications)
            if (a.applicantEmail.equalsIgnoreCase(applicant) && a.opportunityId == opportunityId) return a;
        return null;
    }
    String companyName(String email) {
        Account a = account(email);
        return a == null ? email : a.name;
    }

    /** FR-05 and FR-06: show open opportunities matching every supplied criterion. */
    List<Opportunity> search(String keyword, String location, String field, String type) {
        String k = normalize(keyword), l = normalize(location), f = normalize(field);
        List<Opportunity> result = new ArrayList<>();
        for (Opportunity o : opportunities) {
            if (!o.open) continue;
            if (!normalize(o.title + " " + o.description + " " + o.skills).contains(k)) continue;
            if (!normalize(o.location).contains(l)) continue;
            if (!normalize(o.field).contains(f)) continue;
            if (type != null && !type.equals("All") && !type.equals(o.type)) continue;
            result.add(o);
        }
        return result;
    }
    private static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }

    /** FR-08: only applicant roles may apply, once, to an open opportunity. */
    Application submitApplication(Account applicant, Opportunity opportunity) {
        if (!Member3Security.isApplicant(applicant))
            throw new IllegalArgumentException("Only students and graduates can apply.");
        if (opportunity == null || !opportunity.open || this.opportunity(opportunity.id) != opportunity)
            throw new IllegalArgumentException("This opportunity is not open.");
        if (application(applicant.email, opportunity.id) != null)
            throw new IllegalArgumentException("You have already applied to this opportunity.");
        Application a = new Application(applicant.email, opportunity.id);
        applications.add(a);
        return a;
    }

    /** FR-09/10: only the company owning an opportunity may change its application status. */
    void updateApplicationStatus(Account company, Application app, String status) {
        if (!Member3Security.isCompany(company) || app == null || !applications.contains(app))
            throw new IllegalArgumentException("You cannot edit this application.");
        Opportunity o = opportunity(app.opportunityId);
        if (!Member3Security.ownsOpportunity(company, o))
            throw new IllegalArgumentException("Only the posting company can update this application.");
        if (!Arrays.asList("Under Review", "Accepted", "Rejected").contains(status))
            throw new IllegalArgumentException("Invalid status.");
        app.status = status;
    }
}
class Passwords {
    private static final SecureRandom RANDOM=new SecureRandom();
    static String hash(String password){try{byte[] salt=new byte[16];RANDOM.nextBytes(salt);return Base64.getEncoder().encodeToString(salt)+":"+derive(password,salt);}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean verify(String password,String stored){try{String[] bits=stored.split(":",2);if(bits.length!=2)return false;byte[] actual=Base64.getDecoder().decode(derive(password,Base64.getDecoder().decode(bits[0])));byte[] expected=Base64.getDecoder().decode(bits[1]);return MessageDigest.isEqual(actual,expected);}catch(Exception e){return false;}}
    private static String derive(String pw,byte[] salt)throws Exception {PBEKeySpec spec=new PBEKeySpec(pw.toCharArray(),salt,120000,256);try{return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}finally{spec.clearPassword();}}
}
