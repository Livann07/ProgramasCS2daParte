
package servidorbloqueo;
/**
 *
 * @author livan
 */
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorBloqueo {

    //Todos los hilos van a compartir la información
    //es la única manera, con estáticos
    //Conjunto de nombres, pero no se puede repetir
    private static Set<String>  names = new HashSet<>();
    
    //Se mandan a todos
    private static Set<PrintWriter> writers = new HashSet<>();
    
    private static Map<String,PrintWriter> wr = new HashMap<>();
    //Guardar lista de bloqueados con su set
    private static Map<String, Set> listBloq = new HashMap<>();
    
    
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
       // private static Set<String> bloqueados = new HashSet<>();
            
        //Constructor, que corra rápido
        public Handler(Socket socket){
            this.socket=socket;
        }
            
        public Set agregarBloqueados (String nombre){
           //Se lee el map para obtener los set
           Set sdp = new HashSet();
           Set set = listBloq.entrySet();
           Iterator iterator = set.iterator();
           while(iterator.hasNext()){
               Map.Entry mentry = (Map.Entry) iterator.next();
               if(mentry.getValue() == name){
                     sdp = (Set)mentry.getValue();
                     break;
               }
           }
           
           //Si el set contiene el nombre del que se quiere bloquear, se agrega y se envía el set
           if(!sdp.contains(nombre)){
                 sdp.add(nombre);
           }
           return sdp;
        
        }
        
        
        public Set eliminarBloqueado (String nombre){
           //Se lee el map para obtener los set
           Set sdp = new HashSet();
           Set set = listBloq.entrySet();
           Iterator iterator = set.iterator();
           while (iterator.hasNext()) {
              Map.Entry mentry = (Map.Entry) iterator.next();
              if (mentry.getValue() == name) {
                sdp = (Set)mentry.getValue();
                break;
              }
           }
           //Si el set contiene el nombre del que se quiere eliminar del bloqueo, se remueve y se envía el set
           try{
                 sdp.remove(nombre);
           } catch (Exception e) {
           }
           
           return sdp;
        }
            
        //Checa si está en la lista de bloqueados de cada cliente
        public boolean verificarBloqueados (String nombre){
           Set sdp = new HashSet();
           Set set = listBloq.entrySet();
           Iterator iterator = set.iterator();
           while (iterator.hasNext()) {
              Map.Entry mentry = (Map.Entry) iterator.next();
              if (((String)mentry.getKey()).equalsIgnoreCase(nombre)) {
                sdp = (Set)mentry.getValue();
                break;
              }
           }
           
           if(sdp.contains(name)){
              return true;
           }else{
              return false;
           }
        }
            
        public void run(){
            try{
              in = new Scanner(socket.getInputStream());
              out = new PrintWriter(socket.getOutputStream(),true);
                    
              while(true){
                 //Protocolo
                 out.println("SUBMITNAME");
                 name = in.nextLine();
                 if(name == null){
                      return;
                 }
                 //Nombres guardados en hilo principal
                 //Mientras un hilo checa, que no se meta otro
                 synchronized(names){
                    if(!names.contains(name)){
                      names.add(name);
                      wr.put(name,out);
                      //Agg a listabloqueados
                      listBloq.put(name, null);
                      break;
                    }
                 }
              }
                    
                    
              out.println("NAMEACCEPTED " + name);
              for(PrintWriter writer: writers){
                  writer.println("MESSAGE " + name + " has joined"); 
              }
              //PRIMERO A TODOS Y LUEGO AL QUE SE UNIO
              writers.add(out);
                   
                   
              while(true){
                  
                String input = in.nextLine();
                if(input.toLowerCase().startsWith("/")){
                            
                    if(input.toLowerCase().startsWith("/desbloquear")){
                        String nombre = input.substring(13);
                       // System.out.println(nombre);
                       //Llama metodo eliminar
                        listBloq.replace(name,eliminarBloqueado(nombre));
                    }
                    if(input.toLowerCase().startsWith("/bloquear")){
                        String nombre = input.substring(10);
                        //System.out.println(nombre);
                        //Llama metodo agregar
                        if(!nombre.equalsIgnoreCase(name))
                        listBloq.replace(name,agregarBloqueados(nombre));
                    }
                    if(input.toLowerCase().startsWith("/quit")){
                        return;
                    }
                    try{
                     if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" ")))) {
                        String nombreRecibir = input.substring(1,input.indexOf(" ")); 
                        //Mensajes privados
                        if(!verificarBloqueados(nombreRecibir))
                        wr.get(nombreRecibir).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " **PRIVADO**");
                        wr.get(name).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" "))+ " **PRIVADO**");
                     }
                    }catch(Exception e){
                    }
                }else{
                   
                    for(PrintWriter writer : writers){
                       //Se lee el map para obtener los set
                       String identificado = "";
                       Set set = wr.entrySet();
                       Iterator iterator = set.iterator();
                       while(iterator.hasNext()){
                         Map.Entry mentry = (Map.Entry)iterator.next();
                         if(mentry.getValue() == writer){
                           identificado = mentry.getKey().toString();
                           break;
                         }
                       }
                       
                       Set setDeLaPersona = listBloq.get(identificado);
                       if(setDeLaPersona != null){
                           //Si no está bloqueado, se imprime el mensaje
                           if(!setDeLaPersona.contains(name)){
                              writer.println("MESSAGE " + name + ": " + input);
                           }
                       }else{
                           writer.println("MESSAGE " + name + ": " + input);
                       }
                    }
                            
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
