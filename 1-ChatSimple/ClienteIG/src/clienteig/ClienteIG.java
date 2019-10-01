
package clienteig;
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
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


public class ClienteIG {
    //Variables a utilizar
    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16,50);

        //Constructor
        public ClienteIG(String serverAddress){
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
        
        //Se obtiene el usuario
        private String getName(){
            return JOptionPane.showInputDialog(
            frame, 
            "Choose a screen name:", "Screen name selection", JOptionPane.PLAIN_MESSAGE);
        }
        
        private void run(){
            try{
                try {
                    Socket socket = new Socket(serverAddress, 59001);
                    in = new Scanner(socket.getInputStream());
                    out = new PrintWriter(socket.getOutputStream(), true);
                } catch (IOException s) {
                    System.out.println("Problemas al crear socket " + s.toString());
                }
            
                //Protocolos y llamadas de Frame
                while(in.hasNextLine()){
                  String line = in.nextLine();
                  if(line.startsWith("SUBMITNAME")){
                      out.println(getName());
                  }else if(line.startsWith("NAMEACCEPTED")){
                      this.frame.setTitle("Chatter - " + line.substring(13));
                      textField.setEditable(true);
                  }else if(line.startsWith("MESSAGE")){
                      messageArea.append(line.substring(8) + "\n");
                  }
                }
            }finally{
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
          ClienteIG client = new ClienteIG(args[0]);
          client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          client.frame.setVisible(true);
          client.run();
        
        }
        
}
