package org.strah.client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class UsersPanel extends JPanel {

    private final UserModel model = new UserModel();
    private final PrintWriter out;
    private final BufferedReader in;

    UsersPanel(PrintWriter out, BufferedReader in){
        super(new BorderLayout());
        this.out=out; this.in=in;

        JTable table = new JTable(model);
        JButton bRef = new JButton("Обновить");
        JButton bAdd = new JButton("Добавить");
        JButton bRole= new JButton("Сделать админом");
        JButton bDel = new JButton("Удалить");

        bRef.addActionListener(e -> loadUsers());
        bAdd.addActionListener(e -> addUserDialog());
        bRole.addActionListener(e -> changeRole(table,"Администратор"));
        bDel.addActionListener(e -> deleteUser(table));

        JPanel south=new JPanel();
        south.add(bRef); south.add(bAdd); south.add(bRole); south.add(bDel);

        add(new JScrollPane(table),BorderLayout.CENTER);
        add(south,BorderLayout.SOUTH);

        loadUsers();
    }

    /* ---- команды ---- */
    private void loadUsers(){
        model.clear();
        send("USERS").forEach(model::addFromLine);
    }
    private void addUserDialog(){
        JTextField l=new JTextField(); JPasswordField p=new JPasswordField(); JTextField f=new JTextField();
        String[] roles={"Клиент","Сотрудник","Администратор"};
        JComboBox<String> cb=new JComboBox<>(roles);
        JPanel pan=new JPanel(new GridLayout(4,2));
        pan.add(new JLabel("Логин:")); pan.add(l);
        pan.add(new JLabel("Пароль:")); pan.add(p);
        pan.add(new JLabel("ФИО:"));   pan.add(f);
        pan.add(new JLabel("Роль:"));  pan.add(cb);
        if(JOptionPane.showConfirmDialog(this,pan,"Новый пользователь",
                JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            String cmd="NEWUSER "+l.getText()+" "+new String(p.getPassword())+" "+cb.getSelectedItem()+" "+f.getText().replace(' ','_');
            List<String> r=send(cmd);
            if(r.isEmpty()) loadUsers();
        }
    }
    private void changeRole(JTable t,String role){
        int row=t.getSelectedRow();
        if(row==-1) return;
        String login=(String)model.getValueAt(row,0);
        List<String> r=send("SETROLE "+login+" "+role);
        if(r.isEmpty()) loadUsers();
    }
    private void deleteUser(JTable t){
        int row=t.getSelectedRow();
        if(row==-1) return;
        String login=(String)model.getValueAt(row,0);
        if(JOptionPane.showConfirmDialog(this,"Удалить "+login+"?","",
                JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){
            List<String> r=send("DELUSER "+login);
            if(r.isEmpty()) loadUsers();
        }
    }

    /* ---- обмен с сервером ---- */
    private List<String> send(String cmd){
        List<String> res=new ArrayList<>();
        out.println(cmd);
        try{
            String l;
            while(!(l=in.readLine()).equals("END")){
                if(l.startsWith("ERR")) {                   // показываем и прекращаем
                    JOptionPane.showMessageDialog(this,"Сервер: "+l);
                    return res;
                }
                if(!l.isBlank()) res.add(l);                // только непустые строки
            }
        }catch(Exception e){ JOptionPane.showMessageDialog(this,"Связь потеряна"); }
        return res;
    }

    /* ---- модель таблицы ---- */
    static class UserModel extends javax.swing.table.AbstractTableModel {
        private final List<String[]> rows=new ArrayList<>();
        private final String[] cols={"Логин","Роль","ФИО"};

        void addFromLine(String s){
            String[] parts = s.split(" ");
            if(parts.length < 3)               // пропускаем строки типа EMPTY
                return;
            rows.add(parts);
            fireTableDataChanged();
        }

        void clear(){ rows.clear(); fireTableDataChanged(); }
        @Override public int getRowCount(){ return rows.size(); }
        @Override public int getColumnCount(){ return cols.length; }
        @Override public String getColumnName(int c){ return cols[c]; }
        @Override public Object getValueAt(int r,int c){ return rows.get(r)[c]; }
    }
}
