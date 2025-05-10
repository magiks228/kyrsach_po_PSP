package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/** Окно авторизации / регистрации */
public class AuthFrame extends JFrame {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    /* --- поля формы ВХОД --- */
    private final JTextField tfLoginL  = new JTextField(15);
    private final JPasswordField tfPassL = new JPasswordField(15);

    /* --- поля формы РЕГИСТРАЦИЯ --- */
    private final JTextField tfLoginR  = new JTextField(15);
    private final JPasswordField tfPassR = new JPasswordField(15);
    private final JTextField tfFullName = new JTextField(15);

    private final CardLayout cards = new CardLayout();
    private final JPanel center   = new JPanel(cards);

    public AuthFrame(Socket socket, PrintWriter out, BufferedReader in) {
        super("Вход");
        this.socket = socket; this.out = out; this.in = in;

        center.add(buildLoginCard(), "LOGIN");
        center.add(buildRegCard(),   "REG");

        add(center);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /* ------------------- карточка ЛОГИН ------------------- */
    private JPanel buildLoginCard(){
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = constraints();

        p.add(new JLabel("Логин:"), c);
        c.gridx=1; p.add(tfLoginL,   c);

        c.gridx=0; c.gridy=1; p.add(new JLabel("Пароль:"), c);
        c.gridx=1;            p.add(tfPassL,  c);

        JButton btnLogin = new JButton("Войти");
        btnLogin.addActionListener(e -> doLogin());

        JButton btnToReg = new JButton("Регистрация");
        btnToReg.addActionListener(e -> cards.show(center,"REG"));

        JButton btnExit  = new JButton("Выход");
        btnExit.addActionListener(e -> System.exit(0));

        JPanel south = new JPanel();
        south.add(btnLogin);
        south.add(btnToReg);
        south.add(btnExit);

        p.add(south, southConstraints());
        return p;
    }

    /* ------------------- карточка РЕГИСТРАЦИЯ ------------------- */
    private JPanel buildRegCard(){
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints d = constraints();

        p.add(new JLabel("Логин:"), d);
        d.gridx=1; p.add(tfLoginR, d);

        d.gridx=0; d.gridy=1; p.add(new JLabel("Пароль:"), d);
        d.gridx=1;            p.add(tfPassR, d);

        d.gridx=0; d.gridy=2; p.add(new JLabel("ФИО:"), d);
        d.gridx=1;            p.add(tfFullName, d);

        JButton btnReg = new JButton("Создать");
        btnReg.addActionListener(e -> doRegister());

        JButton btnBack = new JButton("Назад");
        btnBack.addActionListener(e -> cards.show(center,"LOGIN"));

        JButton btnExit = new JButton("Выход");
        btnExit.addActionListener(e -> System.exit(0));

        JPanel south = new JPanel();
        south.add(btnReg); south.add(btnBack); south.add(btnExit);

        p.add(south, southConstraints());
        return p;
    }

    /* ---------------- вход ---------------- */
    private void doLogin(){
        String login=tfLoginL.getText().trim();
        String pass =new String(tfPassL.getPassword());
        if(login.isEmpty()||pass.isEmpty()){ msg("Заполните поля"); return; }

        out.println("LOGIN "+login+" "+pass);
        try{
            String ans=in.readLine();
            if(ans!=null && ans.startsWith("OK")){
                String role=ans.substring(3).trim();
                new MainFrame(socket,out,in,role).setVisible(true);
                dispose();
            }else msg("Ошибка: "+ans);
        }catch(IOException e){ msg("Сервер недоступен"); }
    }

    /* ---------------- регистрация ---------------- */
    private void doRegister(){
        String login=tfLoginR.getText().trim();
        String pass =new String(tfPassR.getPassword());
        String fn   =tfFullName.getText().trim().replace(' ','_');
        if(login.isEmpty()||pass.isEmpty()||fn.isEmpty()){ msg("Заполните все поля"); return; }

        out.println("REGISTER "+login+" "+pass+" "+fn);
        try{
            String ans=in.readLine();
            if("OK".equals(ans)){
                msg("Регистрация успешна. Выполните вход.");
                cards.show(center,"LOGIN");
            }else msg("Ошибка: "+ans);
        }catch(IOException e){ msg("Сервер недоступен"); }
    }

    /* ---------------- util ---------------- */
    private GridBagConstraints constraints(){
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0;
        return c;
    }
    private GridBagConstraints southConstraints(){
        GridBagConstraints s = new GridBagConstraints();
        s.gridx=0; s.gridy=3; s.gridwidth=2;
        s.insets=new Insets(8,4,4,4);
        return s;
    }
    private void msg(String m){ JOptionPane.showMessageDialog(this,m); }
}
