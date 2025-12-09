package Servicio;

import Dominio.Sala;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;

public class Mensaje {

    private UnCliente cliente;
    private ServicioSala ss;

    public Mensaje(UnCliente cliente, ServicioSala ss) {
        this.cliente = cliente;
        this.ss = ss;
    }

    public void manejarEntrada(String mensaje) throws IOException {
        if (mensaje.startsWith("/")) {
            procesarComando(mensaje);
            return;
        }
        procesarChat(mensaje);
    }

    private void procesarChat(String mensaje) {
        Sala salaActual = ss.obtenerSalaDelCliente();

        if (salaActual != null) {
            String formato = "[" + cliente.getId() + " @ " + salaActual.obtenerNombre() + "]: " + mensaje;
            salaActual.broadcast(formato);
            return;
        }
        String formato = "[LOBBY - " + cliente.getId() + "]: " + mensaje;
        ServidorMulti.broadcastGlobal(formato);
    }

    private void procesarComando(String entrada) throws IOException {
        String[] partes = entrada.split(" ");
        String comando = partes[0];
        Sala salaActual = ss.obtenerSalaDelCliente();

        switch (comando) {
            case "/crear":
                ss.crear(cliente);
                break;

            case "/unirse":
                if (partes.length > 1) {
                    ss.unirse(partes[1]);
                    break;
                }
                cliente.salida().writeUTF("Uso correcto: /unirse [nombreSala]");
                break;

            case "/ver":
                ss.ver();
                break;

            case "/salir":
                //  cliente.salida().writeUTF("Función salir pendiente de implementar.");
                break;

            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(salaActual);
                break;

            default:
                cliente.salida().writeUTF("Comando no reconocido: " + comando);
        }
    }
}
