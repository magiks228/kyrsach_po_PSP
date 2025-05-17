package org.strah.server;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.strah.model.applications.Application;
import org.strah.model.applications.ApplicationAnswer;
import org.strah.model.applications.ApplicationStatus;
import org.strah.model.claims.Claim;
import org.strah.model.policies.InsurancePolicy;
import org.strah.model.types.RiskCoeff;
import org.strah.model.users.AppUser;
import org.strah.utils.AuthManager;
import org.strah.utils.HibernateUtil;
import org.strah.model.types.InsuranceType;
import org.strah.model.users.Role;
import org.strah.utils.PremiumCalculator;
import java.time.format.DateTimeParseException;


import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class ClientHandler extends Thread {

    private static final String SEP = "\u001F";

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

                    case "POLICIES" -> handlePolicies(out);
                    case "CLAIMS"  -> handleClaims(out);
                    case "NEWCLAIM"-> handleNewClaim(args,out);

                    /* --- заявки --- */
                    case "NEWAPP"          -> handleNewApp(args, out);
                    case "NEWAPP_ANSWER"   -> handleNewAppAnswer(args, out);
                    case "APPLIST"         -> handleAppList(out);
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

                    case "APPROVE_CLAIM" -> handleClaimStatusChange(args, Claim.Status.APPROVED, out);
                    case "REJECT_CLAIM"  -> handleClaimStatusChange(args, Claim.Status.REJECTED, out);


                    case "CALC" -> handleCalc(args, out);


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
        if (a.length < 3) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }
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
    private void handlePolicies(PrintWriter out) {
        if (notLogged(out)) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<InsurancePolicy> list;

            boolean isStaff = currentUser.getRoleEnum() == Role.ADMIN || currentUser.getRoleEnum() == Role.STAFF;

            if (isStaff) {
                list = session.createQuery("from InsurancePolicy", InsurancePolicy.class).list();
            } else {
                list = session.createQuery(
                                "from InsurancePolicy p where p.customer.id = :cid", InsurancePolicy.class)
                        .setParameter("cid", currentUser.getId())
                        .getResultList();
            }

            if (list.isEmpty()) {
                out.println("EMPTY");
            } else {
                for (InsurancePolicy p : list) {
                    out.println(
                            p.getPolicyNumber() + SEP +
                                    p.getType().getCode() + SEP +
                                    (p.getCoverageAmount() != null ? p.getCoverageAmount() : 0.0) + SEP +
                                    p.getStartDate() + SEP +
                                    p.getEndDate() + SEP +
                                    p.getPremium() + SEP +
                                    p.getCustomer().getLogin()
                    );
                }
            }

            out.println("END");
        } catch (Exception e) {
            out.println("ERR " + e.getMessage());
            out.println("END");
        }
    }




    /* ---------- CLAIMS ---------- */
    private void handleClaims(PrintWriter out) {
        if (notLogged(out)) return;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<Claim> list = s.createQuery(
                    "from Claim c where c.policy is not null", Claim.class
            ).list();

            if (list.isEmpty()) {
                out.println("EMPTY");
            } else {
                final String SEP = "\u001F";
                for (Claim c : list) {
                    InsurancePolicy p = c.getPolicy();

                    // Добавим защиту от проблем с null-полем policy (или если Hibernate не смог инициализировать)
                    if (p == null || p.getPolicyNumber() == null) continue;

                    out.println(
                            c.getId()             + SEP +
                                    p.getPolicyNumber()   + SEP +
                                    c.getAmount()         + SEP +
                                    c.getStatus()
                    );
                }
                out.println("END");
            }
        } catch (Exception ex) {
            ex.printStackTrace(); // оставим для логов
            out.println("ERR " + ex.getMessage());
            out.println("END");
        }
    }



    /* ---------- NEWCLAIM ---------- */
    private void handleNewClaim(String args, PrintWriter out){
        if(notLogged(out)) return;
        String[] a=args.split(" ",3);
        if (a.length < 3) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }
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
            if(pol==null){
                out.println("ERR NoPolicy");
                out.println("END");
                return;
            }

            Transaction tx=s.beginTransaction();
            s.persist( new Claim(pol, LocalDate.now(), amount, descr) );
            tx.commit();
            out.println("OK");
            out.println("END");
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
    private void handleUsers(PrintWriter out) {
        if (requireStaff(out)) {
            out.println("END");    // <--- добавлено
            return;
        }
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<AppUser> list = s.createQuery("from AppUser", AppUser.class).list();
            if (list.isEmpty()) {
                out.println("EMPTY");
            } else {
                for (AppUser u : list) {
                    out.println(
                            u.getLogin() + " " +
                                    u.getRole()  + " " +
                                    u.getFullName().replace(' ', '_')
                    );
                }
            }
        } catch (Exception e) {
            out.println("ERR " + e.getMessage());
        } finally {
            out.println("END");    // <--- гарантируем END
        }
    }



    /* NEWUSER login pass role fullName */
    private void handleNewUser(String args, PrintWriter out){
        if(requireAdmin(out)) return;

        String[] a = args.split(" ", 4);
        if (a.length < 4) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }
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
    // args: num typeCode start end premium coverage clientLogin
    private void handleNewPolicy(String args, PrintWriter out){
        if(requireStaff(out)) return;

        String[] a = args.split(" ");
        if(a.length != 7){
            out.println("ERR Syntax");
            out.println("END");
            return;
        }

        String num = a[0];
        String typeCode = a[1];
        LocalDate startDate, endDate;
        try {
            startDate = LocalDate.parse(a[2], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            endDate   = LocalDate.parse(a[3], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException ex) {
            out.println("ERR Неверный формат даты");
            out.println("END");
            return;
        }

        double premium = Double.parseDouble(a[4]);
        double coverage = Double.parseDouble(a[5]);
        String clientLogin = a[6];

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            if (policyExists(s, num)) {
                out.println("ERR Exists");
                out.println("END");
                return;
            }

            InsuranceType type = s.createQuery("from InsuranceType where code = :c", InsuranceType.class)
                    .setParameter("c", typeCode)
                    .uniqueResult();

            AppUser client = s.byNaturalId(AppUser.class).using("login", clientLogin).load();

            if (type == null || client == null) {
                out.println("ERR NotFound");
                out.println("END");
                return;
            }

            Transaction tx = s.beginTransaction();
            InsurancePolicy p = new InsurancePolicy(num, startDate, endDate, premium, coverage, client, type);
            s.persist(p);
            tx.commit();

            out.println("OK");
        } catch (Exception ex) {
            out.println("ERR " + ex.getMessage());
        } finally {
            out.println("END");
        }
    }



    private void handleNewApp(String args, PrintWriter out) {
        // parts: typeCode months coverage
        String[] parts = args.split(" ");
        if (parts.length != 3) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }

        String typeCode = parts[0];
        int    months   = Integer.parseInt(parts[1]);
        double coverage = Double.parseDouble(parts[2]);

        if (notLogged(out)) return;

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Application app = new Application(
                    currentUser.getId(),
                    parts[0],
                    Double.parseDouble(parts[2]),
                    Integer.parseInt(parts[1])
            );
            s.persist(app);
            tx.commit();
            out.println("OK " + app.getId());
            out.println("END");
        }
    }

    /**
     * Обрабатывает команду NEWAPP_ANSWER <appId> <coeffGroup> <optionCode>
     */
    private void handleNewAppAnswer(String args, PrintWriter out) {
        String[] parts = args.split(" ");
        if (parts.length != 3) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }
        long   appId      = Long.parseLong(parts[0]);
        String coeffGroup = parts[1];
        String optionCode = parts[2];

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();

            Application app = s.get(Application.class, appId);
            if (app == null) {
                out.println("ERR NoApp");
                out.println("END");
                return;
            }

            ApplicationAnswer answer = new ApplicationAnswer(app, coeffGroup, optionCode);
            s.persist(answer);

            tx.commit();
            out.println("OK");
            out.println("END");
        }
    }




    /** ---------- APPLIST (только свои для клиента, или все для staff) ---------- */
    private void handleAppList(PrintWriter out){
        if (notLogged(out)) return;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            String hql = currentUser.getRoleEnum() == Role.CLIENT
                    ? "from Application where customerId = :uid"
                    : "from Application";
            var q = s.createQuery(hql, Application.class);
            if (hql.contains(":uid")) q.setParameter("uid", currentUser.getId());

            List<Application> apps = q.list();
            if (apps.isEmpty()) {
                out.println("EMPTY");
            } else {
                for (Application a : apps) {
                    // <id> SEP <typeCode> SEP <termMonths> SEP <coverageAmount>
                    //    SEP <premium> SEP <status> SEP <startDate> SEP <endDate>
                    out.println(
                            a.getId()           + SEP +
                                    a.getTypeCode()     + SEP +
                                    a.getTermMonths()   + SEP +
                                    a.getCoverageAmount()+ SEP +
                                    (a.getPremium()!=null ? a.getPremium() : 0.0) + SEP +
                                    a.getStatus().name()+ SEP +
                                    (a.getStartDate()!=null ? a.getStartDate() : "-") + SEP +
                                    (a.getEndDate()!=null   ? a.getEndDate()   : "-")
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
            out.println("END");
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
                out.println("END");
                return;
            }
            if (app.getStatus() != ApplicationStatus.WAIT_PAYMENT) {
                out.println("ERR BadStatus");
                out.println("END");
                return;
            }

            Transaction tx = s.beginTransaction();
            app.setStatus(ApplicationStatus.PAID);
            LocalDate start = LocalDate.now();
            LocalDate end   = start.plusMonths(app.getTermMonths());
            app.setStartDate(start);
            app.setEndDate(end);
            tx.commit();

            out.println("OK");
            out.println("END");
        }
    }


    /** --- выпуск полиса из оплаченной заявки --- */
    private void handlePolicyFromApp(String args, PrintWriter out){
        if(requireStaff(out)) return;
        String[] a = args.split(" ");
        if(a.length != 3){ out.println("ERR Syntax"); out.println("END"); return; }

        long appId = Long.parseLong(a[0]);
        LocalDate start = LocalDate.parse(a[1], DF);
        LocalDate end   = LocalDate.parse(a[2], DF);

        try(Session s = HibernateUtil.getSessionFactory().openSession()){
            Application app = s.get(Application.class, appId);
            if(app == null){ out.println("ERR NoApp"); out.println("END"); return; }
            if(app.getStatus() != ApplicationStatus.PAID){
                out.println("ERR NotPaid"); out.println("END"); return;
            }

            InsuranceType type = s.createQuery(
                            "from InsuranceType where code = :c", InsuranceType.class)
                    .setParameter("c", app.getTypeCode())
                    .uniqueResult();
            AppUser client = s.get(AppUser.class, app.getCustomerId());

            String num = type.getCode() + "-" +
                    LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" +
                    String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));

            Transaction tx = s.beginTransaction();
            InsurancePolicy pol = new InsurancePolicy(
                    num,
                    start,
                    end,
                    app.getPremium(),
                    app.getCoverageAmount(),
                    client,
                    type
            );
            pol.setApplication(app);
            app.setStatus(ApplicationStatus.FINISHED);
            s.persist(pol);
            tx.commit();

            out.println("OK");
            out.println("END");
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
            List<InsuranceType> list = s.createQuery("from InsuranceType", InsuranceType.class).list();

            if (list.isEmpty()) {
                System.out.println(">>> [InsuranceTypes] Список пуст");
                out.println("EMPTY");
            } else {
                System.out.println(">>> [InsuranceTypes] Найдено типов: " + list.size());
                for (InsuranceType t : list) {
                    String line = t.getCode() + SEP +
                            t.getNameRu() + SEP +
                            t.getLimitMin() + SEP +
                            t.getLimitMax() + SEP +
                            t.getBaseRateMin() + SEP +
                            t.getBaseRateMax() + SEP +
                            t.getDefaultTerm() + SEP +
                            t.getFranchisePercent();
                    System.out.println(">>> [InsuranceTypes] Отправляем: " + line);
                    out.println(line);
                }
            }

            out.println("END");
            System.out.println(">>> [InsuranceTypes] Отправлен END");
        }
    }


    /** Отдаём client’у коэффициенты */
    private void streamRiskCoeffs(PrintWriter out) {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            List<RiskCoeff> list = s.createQuery("from RiskCoeff", RiskCoeff.class).list();
            if (list.isEmpty()) {
                out.println("EMPTY");
            } else {
                for (RiskCoeff rc : list) {
                    // <typeCode> SEP <group> SEP <optionCode> SEP <optionName> SEP <value>
                    out.println(
                            rc.getTypeCode()   + SEP +
                                    rc.getCoeffGroup()      + SEP +
                                    rc.getOptionCode() + SEP +
                                    rc.getOptionName() + SEP +
                                    rc.getValue()
                    );
                }
            }
            out.println("END");
        }
    }


    /**
     * CALC <appId> — пересчитать premium и сохранить
     */
    private void handleCalc(String args, PrintWriter out) {
        if (requireStaff(out)) return;

        long appId;
        try {
            appId = Long.parseLong(args.trim());
        } catch (NumberFormatException ex) {
            out.println("ERR Syntax");
            out.println("END");
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Application app = s.get(Application.class, appId);
            if (app == null) {
                out.println("ERR NoApp");
                out.println("END");
                return;
            }
            // пересчитываем премию
            double newPremium = new PremiumCalculator().calculate(app, s);
            app.setPremium(newPremium);
            tx.commit();

            out.println("OK " + newPremium);
            out.println("END");
        } catch (Exception e) {
            out.println("ERR " + e.getMessage());
            out.println("END");
        }
    }



    private void handleClaimStatusChange(String args, Claim.Status newStatus, PrintWriter out) {
        if (requireStaff(out)) return;

        long claimId;
        try {
            claimId = Long.parseLong(args.trim());
        } catch (NumberFormatException e) {
            out.println("ERR InvalidID");
            out.println("END");
            return;
        }

        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Claim claim = s.get(Claim.class, claimId);
            if (claim == null) {
                out.println("ERR NoClaim");
                out.println("END");
                return;
            }

            if (claim.getStatus() != Claim.Status.NEW) {
                out.println("ERR AlreadyProcessed");
                out.println("END");
                return;
            }

            InsurancePolicy policy = claim.getPolicy();
            double remaining = policy.getCoverageAmount();

            if (newStatus == Claim.Status.APPROVED && claim.getAmount() > remaining) {
                out.println("ERR OverLimit");
                out.println("END");
                return;
            }

            Transaction tx = s.beginTransaction();

            claim.setStatus(newStatus);

            if (newStatus == Claim.Status.APPROVED) {
                policy.setCoverageAmount(remaining - claim.getAmount());
            }

            tx.commit();

            out.println("OK");
            out.println("END");
        } catch (Exception e) {
            out.println("ERR " + e.getMessage());
            out.println("END");
        }
    }


}


