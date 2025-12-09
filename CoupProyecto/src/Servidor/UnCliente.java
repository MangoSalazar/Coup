package Servidor;

import Servicio.Mensaje;
import Servicio.ServicioSala;
import Servicio.ServicioSesion;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {

    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;
    private String id;
    private ServicioSala servicioSala;
    //
    // para saber si el cliente está en una sala privada
    private boolean enSala = false;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new DataInputStream(socket.getInputStream());
        this.id = id;
        this.servicioSala = new ServicioSala(this);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public DataInputStream entrada() {
        return this.entrada;
    }

    public DataOutputStream salida() {
        return this.salida;
    }

    public boolean isEnSala() {
        return enSala;
    }

    public void setEnSala(boolean enSala) {
        this.enSala = enSala;
    }

    @Override
    public void run() {
        try {
            this.salida.writeUTF("Bienvenido " + id + ". Estás en el Lobby General.");
            this.salida.writeUTF("Usa /crear [nombre] o /unirse [nombre] para jugar.");
            
            ServicioSesion ss = new ServicioSesion(this);
            
            while (true) {
                if (!ss.isSesionIniciada()) {
                    ss.iniciarSesion();
                }
                
                while (true) {
                    // leemos el mensaje y delegamos todo a la clase Mensaje
                    String textoRecibido = entrada.readUTF();
                    Mensaje.manejarEntrada(this, textoRecibido, servicioSala);
                }
            }

        } catch (IOException ex) {
            System.out.println("Cliente " + id + " se desconectó.");
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            ServidorMulti.eliminarIdCliente(id);
        }
    }
}