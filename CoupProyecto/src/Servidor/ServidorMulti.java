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
            broadcastGlobal("El usuario " + idAntiguo + " ahora es " + idNuevo, null);
        }
    }

    public static synchronized void eliminarIdCliente(String id) {
        clientes.remove(id);
        broadcastGlobal("El usuario " + id + " se ha desconectado.", null);
    }

    public static synchronized void broadcastGlobal(String mensaje, UnCliente remitente) {
        for (UnCliente c : clientes.values()) {

            boolean esRemitente = (remitente != null && c.equals(remitente));
            boolean estaEnSala = c.isEnSala();

            boolean sesionIniciada = (c.getServicioSesion() != null && c.getServicioSesion().isSesionIniciada());

            if (!esRemitente && !estaEnSala && sesionIniciada) {
                try {
                    c.salida().writeUTF(mensaje);
                } catch (IOException e) {
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket servidorSocket = new ServerSocket(8080);
        contador = 0;
        System.out.println("Servidor iniciado en puerto 8080...");

        while (true) {
            Socket s = servidorSocket.accept();
            UnCliente unCliente = new UnCliente(s, Integer.toString(contador));
            Thread hilo = new Thread(unCliente);
            clientes.put(Integer.toString(contador), unCliente);
            hilo.start();
            System.out.println("Cliente conectado: " + contador);
            contador++;
        }
    }
}