package Servidor;

import Servicio.Mensaje;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class UnCliente implements Runnable {

    private final Socket socket;
    private final DataInputStream entrada;
    private final DataOutputStream salida;
    private String id;

    public UnCliente(Socket socket, String id) throws IOException {
        this.socket = socket;
        this.salida = new DataOutputStream(socket.getOutputStream());
        this.entrada = new DataInputStream(socket.getInputStream());
        this.id = id;
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

    @Override
    public void run() {
        try {
            this.salida.writeUTF("Conectado como: " + id);
            while (true) {
                String mensajito = entrada.readUTF();
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
