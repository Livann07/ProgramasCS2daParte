
package servidorpassword;
/**
 *
 * @author livan
 */
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorPassword {
    //Todos los hilos van a compartir la información
    //es la única manera, con estáticos
    //Conjunto de nombres, pero no se puede repetir
    private static Set<String>  names = new HashSet<>();
    
    //Se mandan a todos
    private static Set<PrintWriter> writers = new HashSet<>();
    
    private static Map<String,PrintWriter> wr = new HashMap<>();
    //Guardar lista de bloqueados con su set
    private static Map<String, Set> listBloq = new HashMap<>();
    //Guardar lista de usuarios
    private static Map<String,String> listaUsuarios = new HashMap<>();
    
    //Obtener ruta de ejecución
    public String ruta(){
        URL link = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        return link.toString();
    }
    
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
        //Variable a utilizar
        private String password;         
        private String name;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;
        //private static Set<String> bloqueados = new HashSet<>();
            
        //Ruta limpia 
        ServidorPassword sp = new ServidorPassword();
        String pr= sp.ruta();
        int inicio = pr.indexOf(":") +2;
        int nc = pr.indexOf("ServidorPassword") + 17;
        String ruta = pr.substring(inicio,nc);
            
            
        //Constructor, que corra rápido
        public Handler(Socket socket){
                this.socket=socket;
        }
            
        public Set agregarBloqueados (String nombre){
            //Se lee el map para obtener los set
           Set sdp = new HashSet();
           Set set = listBloq.entrySet();
           Iterator iterator = set.iterator();
           while (iterator.hasNext()) {
              Map.Entry mentry = (Map.Entry) iterator.next();
              if (mentry.getKey() == name) {
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
               if (mentry.getKey() == name) {
                  sdp = (Set)mentry.getValue();
                  break;
               }
           }
           //Si el set contiene el nombre del que se quiere eliminar del bloqueo, se remueve y se envía el set  
           try {
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
                //Lee archivo de bloqueados y lo asigna al map 
                try{
                   //System.out.println("ruta 1: " + ruta);
                   FileInputStream fis = new FileInputStream(ruta + "listabloqueados.conf");
                   ObjectInputStream oi = new ObjectInputStream(fis);
                   listBloq = (HashMap)oi.readObject();
                   oi.close();
                   fis.close();
                }catch(Exception e){
                }
                //Lee archivo de usuarios y lo asigna al map   
                try{
                   //System.out.println("ruta 1: " + ruta);
                   FileInputStream fin = new FileInputStream(ruta + "listaUsuarios.conf");
                   ObjectInputStream ob = new ObjectInputStream(fin);
                   listaUsuarios = (HashMap)ob.readObject();
                   ob.close();
                   fin.close();
                }catch(Exception e){
                }
                    
                boolean co=false;
                while(true){
                   //Protocolo
                    if(!co){
                       out.println("SUBMITNAME");
                       name = in.nextLine();
                       if(name == null || name.contains(" ") || name.length()==0){
                          return;
                       }
                    }
                    //Nombres guardados en hilo principal
                    //Mientras un hilo checa, que no se meta otro
                    synchronized(listaUsuarios){
                       if(!listaUsuarios.containsKey(name)){
                           names.add(name);
                           wr.put(name,out);
                           if(!listBloq.containsKey(name))
                           listBloq.put(name, new HashSet());
                              
                           out.println("PASSWORD");
                                
                           password = in.nextLine();
                           if(password == null || password.contains(" ") || password.length()==0){
                               return;
                           }else{
                             // System.out.println("Password " + password);
                             //Guardando usuario
                              listaUsuarios.put(name,password);
                              try{
                                 FileOutputStream fout = new FileOutputStream(ruta + "listaUsuarios.conf");
                                 ObjectOutputStream oout= new ObjectOutputStream(fout);
                                 oout.writeObject(listaUsuarios);
                                 oout.close();
                                 fout.close();
                              }catch(Exception e){
                              }
                                  
                            } 
                                break;
                       }else{
                          names.add(name);
                          //wr.put(name, out);
                          wr.replace(name,out);
                          if(!listBloq.containsKey(name))
                             listBloq.put(name, new HashSet());
                             out.println("PASSWORD");
                             password = in.nextLine();
                            
                             if(password == null || password.contains(" ") || password.length()==0){
                                return;
                             }else{
                                String w = listaUsuarios.get(name);
                                if(w.equalsIgnoreCase(password)){
                                   //System.out.println("Contraseña correcta");
                                   break;
                                }else{
                                   //System.out.println("Contraseña incorrecta");
                                       co=true;
                                }
                             }
                                
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
                   if(input.toLowerCase().startsWith("/")) {
                       if(input.toLowerCase().startsWith("/desbloquear")){
                           String nombre = input.substring(13);
                           //System.out.println(nombre);
                           //System.out.println("La ruta : " + ruta);
                           //Guarda en archivo (act de desbloqueados)
                           synchronized(listBloq.replace(name,eliminarBloqueado(nombre))){
                               try{
                                 FileOutputStream fout = new FileOutputStream(ruta + "listabloqueados.conf");
                                 ObjectOutputStream oout = new ObjectOutputStream(fout);
                                 oout.writeObject(listBloq);
                                 oout.close();
                                 fout.close();
                               }catch(Exception e){
                               }
                           }
                                            
                       }
                       if(input.toLowerCase().startsWith("/bloquear")){
                           String nombre = input.substring(10);
                           //Guarda en archivo los bloqueados
                           if(!nombre.equalsIgnoreCase(name)){
                            synchronized(listBloq.replace(name,agregarBloqueados(nombre))){
                              try{
                                FileOutputStream fout = new FileOutputStream(ruta + "listabloqueados.conf");
                                ObjectOutputStream oout= new ObjectOutputStream(fout);
                                oout.writeObject(listBloq);
                                oout.close();
                                fout.close();
                              }catch(Exception e){
                              }
                            }
                            
                           }
                       }
                       if(input.toLowerCase().startsWith("/quit")){
                           return;
                       }
                       try{
                        if(input.toLowerCase().startsWith("/") && names.contains(input.substring(1,input.indexOf(" ")))) {
                           String nombreRecibir = input.substring(1,input.indexOf(" ")); 
                           //Mensajes privados
                           if(!verificarBloqueados(nombreRecibir))
                           wr.get(nombreRecibir).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" ")) + " **PRIVADO** ");
                           wr.get(name).println("MESSAGE " + name + ": " + input.substring(input.indexOf(" "))+ " **PRIVADO** ");
                        }
                       }catch(Exception e){
                       }
                       
                   }else{
                        //Proceso para imprimir a todos, con la excepción de los bloqueados
                       for(PrintWriter writer : writers){
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
