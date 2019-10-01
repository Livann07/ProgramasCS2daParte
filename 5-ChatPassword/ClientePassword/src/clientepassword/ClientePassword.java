
package clientepassword;
/**
 *
 * @author livan
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;



public class ClientePassword {
    //Variables a utilizar
    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16,50);
    
    
    //Constructor
    public ClientePassword(String serverAddress){
        this.serverAddress = serverAddress;
          
          textField.setEditable(false);
          messageArea.setEditable(false);
          frame.getContentPane().add(textField,BorderLayout.SOUTH);
          frame.getContentPane().add(new JScrollPane(messageArea),BorderLayout.CENTER);
          frame.pack();
        
          //Hace que escriba lo que escribio en el text field, y lo elimina
          textField.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e){
                 out.println(textField.getText());
                 textField.setText("");
             }     
          });
        
    }
    //Obtener usuario
    private String getName(){
            return JOptionPane.showInputDialog(
            frame, 
            "Choose a screen name:", "Screen name selection", JOptionPane.PLAIN_MESSAGE);
    }
    //Obtener contraseña
    private String getPass(){
        String pw ="";
        Box box = Box.createVerticalBox();
        JLabel t = new JLabel("Password");
        JPanel jp = new JPanel();
        jp.setLayout(new BoxLayout(jp, BoxLayout.X_AXIS));
        jp.add(t);
        jp.add(Box.createHorizontalGlue());
        box.add(jp);
        
        JPasswordField pf = new JPasswordField(4);
        pf.requestFocusInWindow();
        box.add(pf);
        
        pf.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                event.getComponent().requestFocusInWindow();
            }

            @Override
            public void ancestorRemoved(AncestorEvent ae) {
            }

            @Override
            public void ancestorMoved(AncestorEvent ae) {
            }
        });
        
        int va = JOptionPane.showConfirmDialog(frame,box, "Password selection", JOptionPane.PLAIN_MESSAGE,-1,null);
        if(va==0){
            char[] pwo = pf.getPassword();
            pw = new String(pwo);
        }

        return pw;
    }
        
    private void run(){
        try {
           //Creación de sockets
           try{
             Socket socket = new Socket(serverAddress, 59001);
             in = new Scanner(socket.getInputStream());
             out = new PrintWriter(socket.getOutputStream(),true);
           }catch(IOException s){
             System.out.println("Problema del socket " + s.toString());
           }
           //Protocolos y llamadas de Frame 

           while (in.hasNextLine()) {
                String line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("PASSWORD")) {
                    out.println(getPass());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                } else if (line.startsWith("MESSAGE")) {
                    messageArea.append(line.substring(8) + "\n");
                }
           }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
        
        
    public static void main(String[] args){
         //Si la ip no se captura, arroja el mensaje
         if(args.length!=1){
            System.err.println("Pass the server IP as the source arguments");
            return;
         }
         //Llamada al run
         ClientePassword client = new ClientePassword(args[0]);
         client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         client.frame.setVisible(true);
         client.run();
    }
    
}
