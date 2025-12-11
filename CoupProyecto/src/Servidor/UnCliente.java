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

    // Convertimos a variable de clase para acceso externo (getter)
    private ServicioSesion servicioSesion;

    // para saber si el cliente está en una sala privada
    private boolean enSala = false;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new DataInputStream(socket.getInputStream());
        this.id = id;
        this.servicioSala = new ServicioSala(this);
        this.servicioSesion = new ServicioSesion(this);
    }

    // Getter necesario para que ServidorMulti verifique si está logueado
    public ServicioSesion getServicioSesion() {
        return servicioSesion;
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
            while (true) {

                // SI NO HAY SESIÓN, REGRESAR A LOGIN INMEDIATAMENTE
                if (!servicioSesion.isSesionIniciada()) {

                    // NO MANDES NADA AQUÍ, SOLO LOGIN DIRECTO
                    if (servicioSesion.iniciarSesion()) {
                        Mensaje.enviarBienvenida(this);
                    }
                    continue;
                }

                // SOLO SI LA SESIÓN ESTÁ INICIADA
                String textoRecibido = entrada.readUTF();

                // Maneja comandos
                new Mensaje(this, servicioSala).manejarEntrada(textoRecibido);
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
