package org.strah.server;

import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.strah.model.applications.Application;
import org.strah.model.applications.ApplicationStatus;
import org.strah.model.claims.Claim;
import org.strah.model.policies.InsurancePolicy;
import org.strah.model.policies.StandardPolicy;
import org.strah.model.types.RiskCoeff;
import org.strah.model.users.AppUser;
import org.strah.utils.AuthManager;
import org.strah.utils.HibernateUtil;
import org.strah.model.types.InsuranceType;
import org.strah.model.users.Role;
import org.strah.utils.PremiumCalculator;


import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler extends Thread {

    private final Socket socket;
    private final AuthManager auth = new AuthManager();
    private AppUser currentUser;                 // авторизованный пользователь
    private static final DateTimeFormatter DF =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");


    public ClientHandler(Socket socket){ this.socket = socket; }

    @Override public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(),true))
        {
            out.println("READY");

            String line;
            while ((line = in.readLine()) != null) {
                String[] p   = line.split(" ",2);
                String cmd   = p[0].toUpperCase();
                String args  = p.length>1 ? p[1] : "";

                switch (cmd) {
                    case "LOGIN"   -> handleLogin(args,out);
                    case "REGISTER"-> handleRegister(args,out);

                    case "POLICIES"-> handlePolicies(out);
                    case "CLAIMS"  -> handleClaims(out);
                    case "NEWCLAIM"-> handleNewClaim(args,out);

                    /* --- заявки --- */
                    case "NEWAPP"      -> handleNewApp(args,out);
                    case "APPLIST"     -> handleAppList(out);
                    case "APPROVE"     -> handleApprove(args,out);
                    case "DECLINE"     -> handleDecline(args,out);
                    case "PAY"         -> handlePay(args,out);
                    case "NEWPOLICYFROMAPP" -> handlePolicyFromApp(args,out);

                    /* --- управление пользователями/полисами (staff) --- */
                    case "USERS"     -> handleUsers(out);
                    case "NEWUSER"   -> handleNewUser(args,out);
                    case "SETROLE"   -> handleSetRole(args,out);
                    case "DELUSER"   -> handleDelUser(args.trim(),out);
                    case "NEWPOLICY" -> handleNewPolicy(args,out);

                    case "TYPES", "INTYPE_LIST"  -> streamInsuranceTypes(out);
                    case "COEFFS", "INCOEFF_LIST" -> streamRiskCoeffs(out);


                    case "EXIT"      -> { out.println("BYE"); socket.close(); return; }
                    default          -> out.println("ERR Unknown");
                }
            }
        } catch (IOException e){ System.err.println("client I/O: "+e); }
    }

    /* ---------- LOGIN ---------- */
    private void handleLogin(String args, PrintWriter out){
        String[] a = args.split(" ");
        if(a.length != 2){ out.println("ERR"); return; }
        String login = a[0], pass = a[1];

        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            currentUser = s.byNaturalId(AppUser.class)
                    .using("login", login)
                    .load();

            /* 1.  нет в БД, но есть в AuthManager -> заносим в БД */
            if(currentUser == null && auth.authenticate(login, pass)){
                currentUser = new AppUser(login, pass, login,
                        auth.getUserRole(login));
                Transaction tx = s.beginTransaction();
                s.persist(currentUser); tx.commit();
            }
            /* 2.  найден в БД — сверяем пароль */
            else if(currentUser != null){
                if(!currentUser.checkPassword(pass)){
                    out.println("ERR"); return;
                }
            }
            /* 3.  иначе — ошибочная пара логин/пароль */
            else {
                out.println("ERR"); return;
            }
        }
        out.println("OK " + currentUser.getRole());
    }

    /* ---------- REGISTER (Client -> "Клиент") ---------- */
    private void handleRegister(String args, PrintWriter out){
        String[] a=args.split(" ",3);
        if(a.length<3){ out.println("ERR"); return; }
        String login=a[0], pass=a[1], fn=a[2].replace('_',' ');
        try(Session s=HibernateUtil.getSessionFactory().openSession()){
            if(userExists(s, login)){ out.println("ERR Exists"); return; }
            Transaction tx=s.beginTransaction();
            s.persist(new AppUser(login, pass, fn, Role.CLIENT));
            tx.commit();
            out.println("OK");
        }
    }

    /* ---------- POLICIES (only own) ---------- */
    private void handlePolicies(PrintWriter out){
        if (notLogged(out)) return;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            String hql = currentUser.getRoleEnum() == Role.CLIENT
                    ? "from InsurancePolicy p where p.customer.id = :uid"
                    : "from InsurancePolicy";

            var q = s.createQuery(hql, InsurancePolicy.class);
            if (hql.contains(":uid"))
                q.setParameter("uid", currentUser.getId());

            List<InsurancePolicy> list = q.list();
            if (list.isEmpty()) out.println("EMPTY");
            else for (InsurancePolicy p : list)
                out.println(p.toListRow());
            out.println("END");
        }
    }

    /* ---------- CLAIMS ---------- */
    private void handleClaims(PrintWriter out){
        if (notLogged(out)) return;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {

            String hql = currentUser.getRoleEnum() == Role.CLIENT
                    ? "from Claim c where c.policy.customer.id = :uid"
                    : "from Claim";

            var q = s.createQuery(hql, Claim.class);
            if (hql.contains(":uid"))
                q.setParameter("uid", currentUser.getId());

            List<Claim> list = q.list();
            if (list.isEmpty()) out.println("EMPTY");
            else for (Claim c : list)
                out.println(c.getId() + " " +
                        c.getPolicy().getPolicyNumber() + " " +
                        c.getAmount() + " " +
                        c.getStatus());
            out.println("END");
        }
    }

    /* ---------- NEWCLAIM ---------- */
    private void handleNewClaim(String args, PrintWriter out){
        if(notLogged(out)) return;
        String[] a=args.split(" ",3);
        if(a.length<3){ out.println("ERR"); return; }
        String polNum=a[0];
        double amount;
        try{ amount=Double.parseDouble(a[1]); }catch(NumberFormatException e){ out.println("ERR"); return; }
        String descr=a[2].replace('_',' ');

        try(Session s=HibernateUtil.getSessionFactory().openSession()){
            String hql = currentUser.getRole().equalsIgnoreCase("Клиент")
                    ? "from InsurancePolicy p where p.policyNumber = :p and p.customer.id = :uid"
                    : "from InsurancePolicy p where p.policyNumber = :p";

            var q = s.createQuery(hql, InsurancePolicy.class)
                    .setParameter("p", polNum);
            if (hql.contains(":uid")) {          // фильтр нужен только клиенту
                q.setParameter("uid", currentUser.getId());
            }

            InsurancePolicy pol = q.uniqueResult();
            if(pol==null){ out.println("ERR NoPolicy"); return; }
            Transaction tx=s.beginTransaction();
            s.persist( new Claim(pol, LocalDate.now(), amount, descr) );
            tx.commit();
            out.println("OK");
        }
    }

    /* =================================================================== */
    /* ---------------------- АДМИН-КОМАНДЫ ------------------------------ */
    /* =================================================================== */

    private boolean requireAdmin(PrintWriter out){
        if(currentUser==null){ out.println("ERR NotLogged"); return true; }
        if(currentUser.getRoleEnum() != Role.ADMIN) {
            out.println("ERR Forbidden");
            return true;
        }
        return false;
    }

    /* USERS: login role fullName */
    private void handleUsers(PrintWriter out){
        if(requireAdmin(out)) return;
        try(Session s=HibernateUtil.getSessionFactory().openSession()){
            List<AppUser> list=s.createQuery("from AppUser", AppUser.class).list();
            if(list.isEmpty()) out.println("EMPTY");
            else for(AppUser u:list)
                out.println(u.getLogin()+" "+u.getRole()+" "+u.getFullName().replace(' ','_'));
            out.println("END");
        }
    }


    /* NEWUSER login pass role fullName */
    private void handleNewUser(String args, PrintWriter out){
        if(requireAdmin(out)) return;

        String[] a = args.split(" ", 4);
        if (a.length < 4){ out.println("ERR"); return; }

        String login = a[0], pass = a[1], roleStr = a[2],
                fn    = a[3].replace('_',' ');

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());   // ADMIN / STAFF / CLIENT
        } catch (IllegalArgumentException ex){
            out.println("ERR Role"); out.println("END");
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            if (userExists(s, login)) {
                out.println("ERR Exists"); return;
            }

            Transaction tx = s.beginTransaction();
            s.persist(new AppUser(login, pass, fn, role));
            tx.commit();

            out.println("OK");
            out.println("END");
        }
    }


    /* SETROLE login role */
    private void handleSetRole(String args, PrintWriter out){
        if(requireAdmin(out)) return;

        String[] a = args.split(" ");
        if (a.length != 2) { out.println("ERR"); return; }

        String login = a[0], roleStr = a[1];

        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e){
            out.println("ERR Role"); out.println("END");
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            AppUser u = s.byNaturalId(AppUser.class)
                    .using("login", login).load();
            if (u == null) { out.println("ERR NoUser"); return; }

            Transaction tx = s.beginTransaction();
            u.setRole(role);
            tx.commit();

            out.println("OK");
            out.println("END");
        }
    }

    /* DELUSER login */
    private void handleDelUser(String login, PrintWriter out){
        if(requireAdmin(out)) return;

        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            AppUser u = s.byNaturalId(AppUser.class)
                    .using("login", login).load();
            if(u == null){ out.println("ERR NoUser"); out.println("END"); return; }

            Long cnt = s.createQuery(
                            "select count(p) from InsurancePolicy p where p.customer.id = :uid", Long.class)
                    .setParameter("uid", u.getId()).uniqueResult();

            if(cnt != null && cnt > 0){
                out.println("ERR HasPolicies");
                out.println("END");
                return;
            }
            Transaction tx = s.beginTransaction();
            s.remove(u); tx.commit();
            out.println("OK");
            out.println("END");
        }
    }

    /* ============ NEWPOLICY (ручное) — использует InsuranceType =========== */
    private void handleNewPolicy(String args, PrintWriter out){
        if(requireStaff(out)) return;

        String[] a = args.split(" ");
        if(a.length!=6){ out.println("ERR Syntax"); return; }

        String num       = a[0];
        String typeCode  = a[1];
        LocalDate start  = LocalDate.parse(a[2],DF);
        LocalDate end    = LocalDate.parse(a[3],DF);
        double premium   = Double.parseDouble(a[4]);
        String clientLog = a[5];

        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            if(policyExists(s,num)){ out.println("ERR Exists"); return; }

            InsuranceType type = s.createQuery(
                            "from InsuranceType where code=:c", InsuranceType.class)
                    .setParameter("c", typeCode).uniqueResult();
            if(type==null){ out.println("ERR NoType"); return; }

            AppUser client = s.byNaturalId(AppUser.class)
                    .using("login", clientLog).load();
            if(client==null){ out.println("ERR NoClient"); return; }

            Transaction tx=s.beginTransaction();
            InsurancePolicy p = new StandardPolicy(
                    num, start, end, premium, client, type);
            s.persist(p); tx.commit();
            out.println("OK");
        }
    }

    private void handleNewApp(String args, PrintWriter out) {
        // args: "typeCode months coverage"
        String[] parts = args.split(" ");
        if (parts.length != 3) { out.println("ERR Syntax"); return; }

        String typeCode = parts[0];
        int    months   = Integer.parseInt(parts[1]);
        double coverage = Double.parseDouble(parts[2]);

        if (notLogged(out)) return;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            // создаём заявку
            Application app = new Application(
                    currentUser.getId(),   // customerId
                    typeCode,
                    coverage,
                    months
            );
            s.persist(app);

            tx.commit();
            out.println("OK " + app.getId());
        }
    }



    /** ---------- APPLIST (только свои для клиента, или все для staff) ---------- */
    private void handleAppList(PrintWriter out) {
        if (notLogged(out)) return;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            String hql = currentUser.getRoleEnum() == Role.CLIENT
                    ? "from Application where customerId = :uid"
                    : "from Application";

            var q = s.createQuery(hql, Application.class);
            if (hql.contains(":uid")) {
                q.setParameter("uid", currentUser.getId());
            }

            List<Application> apps = q.list();
            if (apps.isEmpty()) {
                out.println("EMPTY");
            } else {
                for (Application a : apps) {
                    out.printf("%d %s %d %.2f %.2f %s %s %s%n",
                            a.getId(),
                            a.getTypeCode(),
                            a.getTermMonths(),
                            a.getCoverageAmount(),
                            a.getPremium() != null ? a.getPremium() : 0.0,
                            a.getStatus().name(),
                            a.getStartDate() != null ? a.getStartDate().toString() : "-",
                            a.getEndDate()   != null ? a.getEndDate().toString()   : "-"
                    );
                }
            }
            out.println("END");
        }
    }


    /* ---------- APPROVE / DECLINE ---------- */
    private void handleApprove(String id, PrintWriter out) {
        changeStatus(id, ApplicationStatus.WAIT_PAYMENT, out);
    }
    private void handleDecline(String id, PrintWriter out) {
        changeStatus(id, ApplicationStatus.DECLINED, out);
    }

    private void changeStatus(String id, ApplicationStatus newStatus, PrintWriter out) {
        if (requireStaff(out)) return;
        long appId = Long.parseLong(id);

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Application app = s.get(Application.class, appId);
            if (app == null) {
                out.println("ERR NoApp");
                return;
            }

            Transaction tx = s.beginTransaction();
            // при переходе в ожидание оплаты пересчитываем премию
            if (newStatus == ApplicationStatus.WAIT_PAYMENT) {
                double recalculated = new PremiumCalculator().calculate(app, s);
                app.setPremium(recalculated);
            }
            app.setStatus(newStatus);
            tx.commit();

            out.println("OK");
        }
    }


    /** --- оплата клиентом --- */
    private void handlePay(String id, PrintWriter out) {
        if (notLogged(out)) return;
        long appId = Long.parseLong(id);

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Application app = s.get(Application.class, appId);
            if (app == null || !app.getCustomerId().equals(currentUser.getId())) {
                out.println("ERR NoApp");
                return;
            }
            // здесь проверяем именно ApplicationStatus.WAIT_PAYMENT
            if (app.getStatus() != ApplicationStatus.WAIT_PAYMENT) {
                out.println("ERR BadStatus");
                return;
            }

            Transaction tx = s.beginTransaction();
            app.setStatus(ApplicationStatus.PAID);
            tx.commit();

            out.println("OK");
        }
    }

    /** --- выпуск полиса из оплаченной заявки --- */
    private void handlePolicyFromApp(String args, PrintWriter out) {
        if (requireStaff(out)) return;
        String[] a = args.split(" ");
        if (a.length != 3) { out.println("ERR Syntax"); return; }

        long appId = Long.parseLong(a[0]);
        LocalDate start = LocalDate.parse(a[1], DF);
        LocalDate end   = LocalDate.parse(a[2], DF);

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Application app = s.get(Application.class, appId);
            if (app == null) { out.println("ERR NoApp"); return; }
            if (app.getStatus() != ApplicationStatus.PAID) {
                out.println("ERR NotPaid"); return;
            }

            // получаем тип и клиента
            InsuranceType type = s.createQuery(
                            "from InsuranceType where code = :c", InsuranceType.class)
                    .setParameter("c", app.getTypeCode())
                    .uniqueResult();
            AppUser client = s.get(AppUser.class, app.getCustomerId());

            // генерируем номер
            String num = type.getCode() + "-" +
                    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                    String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));

            Transaction tx = s.beginTransaction();
            InsurancePolicy pol = new StandardPolicy(
                    num, start, end, app.getPremium(), client, type
            );
            pol.setApplication(app);
            app.setStatus(ApplicationStatus.FINISHED);
            s.persist(pol);
            tx.commit();

            out.println("OK");
        }
    }

    /* ---------- helpers ---------- */
    private boolean requireStaff(PrintWriter out){
        if(currentUser==null){ out.println("ERR NotLogged"); return true; }
        Role r = currentUser.getRoleEnum();
        if(r == Role.ADMIN || r == Role.STAFF) return false;
        out.println("ERR Forbidden");
        return true;
    }

    /* ======== расчёт премии через коэффициенты ======== */
    private double calcPremium(Application a){
        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            return new PremiumCalculator().calculate(a, s);
        }
    }


    /* =================================================================== */

    private boolean notLogged(PrintWriter out){
        if(currentUser==null){ out.println("ERR NotLogged"); return true; }
        return false;
    }

    private boolean userExists(Session s,String login){
        Long cnt = s.createQuery("select count(u) from AppUser u where u.login=:l", Long.class)
                .setParameter("l",login).uniqueResult();
        return cnt!=null && cnt>0;
    }

    /* проверка уникальности номера полиса */
    private boolean policyExists(Session s,String num){
        Long cnt = s.createQuery(
                "select count(p) from InsurancePolicy p where p.policyNumber=:n",
                Long.class).setParameter("n",num).uniqueResult();
        return cnt!=null && cnt>0;
    }


    private void handleTypes(PrintWriter out){
        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            List<InsuranceType> list = s.createQuery(
                    "from InsuranceType", InsuranceType.class).list();
            if(list.isEmpty()){ out.println("EMPTY"); return; }

            for(InsuranceType t : list){
                out.printf("%s %.4f %d %.0f %.0f%n",
                        t.getCode(),
                        (t.getBaseRateMin()+t.getBaseRateMax())/2.0,
                        t.getDefaultTerm(),
                        t.getLimitMin(),
                        t.getLimitMax());
            }
            out.println("END");
        }
    }


    /* ==== передаём справочники клиенту ==== */
    private void streamInsuranceTypes(PrintWriter out) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            s.createQuery("from InsuranceType", InsuranceType.class)
                    .getResultList()
                    .forEach(t -> out.println(
                            t.getCode()      + " " +
                                    t.getBaseRateMin()+ " " +
                                    t.getDefaultTerm()+ " " +
                                    t.getLimitMin()  + " " +
                                    t.getLimitMax()
                    ));
            out.println("END");
        }
    }

    /** Отдаём client’у коэффициенты */
    private void streamRiskCoeffs(PrintWriter out) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            s.createQuery("from RiskCoeff", RiskCoeff.class)
                    .getResultList()
                    .forEach(rc -> out.println(
                            rc.getTypeCode()  + " " +
                                    rc.getGroup()     + " " +
                                    rc.getOptionCode()+ " " +
                                    rc.getOptionName()+ " " +
                                    rc.getValue()
                    ));
            out.println("END");
        }
    }

}


