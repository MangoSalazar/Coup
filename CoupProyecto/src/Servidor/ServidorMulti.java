package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServidorMulti {
    public static HashMap<String,UnCliente> clientes = new HashMap<>();
    static int contador;
    public static synchronized void cambiarIdCliente(String idAntiguo, String idNuevo) {
    UnCliente cliente = clientes.remove(idAntiguo);
    if (cliente != null) {
        cliente.setId(idNuevo);
        clientes.put(idNuevo, cliente);
        System.out.println("Cliente " + idAntiguo + " ahora es " + idNuevo);
    }
}
    public static synchronized void eliminarIdCliente(String id) {
        clientes.remove(id);
    }


    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
            contador = 0;
        while (true) {            
            Socket s = servidorSocket.accept();
            UnCliente unCliente = new UnCliente(s,Integer.toString(contador));
            Thread hilo = new Thread(unCliente);
            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();
            System.out.println("Cliente: "+contador);
            contador++;
        }
        
        
        
        
    }
    
}
