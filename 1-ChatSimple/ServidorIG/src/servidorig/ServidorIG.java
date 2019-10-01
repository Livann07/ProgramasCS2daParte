
package servidorig;
/**
 *
 * @author livan
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorIG {
    
    //Todos los hilos van a compartir la información
    //es la única manera, con estáticos
    //Conjunto de nombres, pero no se puede repetir
    private static Set<String>  names = new HashSet<>();
    
    //Se mandan a todos
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args){
       
        System.out.println("The chat server is running...");
        //hilos, usuarios
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try(ServerSocket listener = new ServerSocket(59001)){
            while(true){
                //crear un nuevo hilo, para aceptar cuando se conecten
                pool.execute(new Handler(listener.accept()));
            }
        }catch(Exception e){
            System.out.println("Problema al ejecutar " + e.toString());
        }
    }
    
    private static class Handler implements Runnable{
      //Variables a utilizar
      private String name;
      private Socket socket;
      private Scanner in;
      private PrintWriter out;
            
      //Constructor, que corra rápido
      public Handler(Socket socket){
          this.socket=socket;
      }
            
      public void run(){
          try{
              in = new Scanner(socket.getInputStream());
              out = new PrintWriter(socket.getOutputStream(),true);
                    
              while(true){
                 //Protocolo
                 out.println("SUBMITNAME");
                 try{
                     name = in.nextLine();
                 }catch(Exception e){
                     System.out.println("Problema al leer " +e.toString());
                 }
                 if(name == null){
                     return;
                 }
                 //Nombres guardados en hilo principal
                 //Mientras un hilo checa, que no se meta otro
                 synchronized(names){
                    if(!names.contains(name)){
                         names.add(name);
                         break;
                    }
                 }
              }
                    
              out.println("NAMEACCEPTED " + name);
              for(PrintWriter writer: writers){
                    writer.println("MESSAGE " + name + " has joined"); 
              }
              //PRIMERO A TODOS Y LUEGO AL QUE SE UNIO, es la confirmación
              writers.add(out);
                    
              while(true){
                  String input = in.nextLine();
                  //Protocolo para salir
                  if(input.toLowerCase().startsWith("/quit")){
                     return;
                  }
                  //Escribe a todos el mensaje                        
                  for(PrintWriter writer : writers){
                     writer.println("MESSAGE " + name + ": " + input);
                  }
                         
              }
          }catch(Exception e){
                System.out.println(e);
          }finally{
             if(out!=null){
                 writers.remove(out);
             }        
             if(name!=null){
                 System.out.println(name + " is leaving");
                 names.remove(name);
                 for(PrintWriter writer : writers){
                     writer.println("MESSAGE " + name + " has left");
                 }
             }
             try{ socket.close();} catch(IOException e){}
          }
            
      }
      
    }
}
