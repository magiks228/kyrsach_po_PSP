package org.strah.client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ClientStarter {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            try{
                Socket sock=new Socket("localhost",8080);
                PrintWriter out=new PrintWriter(sock.getOutputStream(),true);
                BufferedReader in=new BufferedReader(new InputStreamReader(sock.getInputStream()));
                if(!"READY".equals(in.readLine()))
                    throw new IOException("Сервер не ответил READY");

                new AuthFrame(sock,out,in).setVisible(true);
            }catch(Exception e){
                JOptionPane.showMessageDialog(null,"Сервер недоступен:\n"+e.getMessage());
            }
        });
    }
}
