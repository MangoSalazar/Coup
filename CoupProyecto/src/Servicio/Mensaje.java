package Servicio;

import Dominio.Partida;
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
            String formato = "[" + cliente.getId() + "]: " + mensaje;
            salaActual.broadcast(formato, cliente);
            return;
        }
        String formato = "[LOBBY - " + cliente.getId() + "]: " + mensaje;
        ServidorMulti.broadcastGlobal(formato, cliente);

    }

    private void procesarComando(String mensaje) throws IOException {
        String[] partes = mensaje.split(" ");
        String comando = partes[0];
        Sala salaActual = ss.obtenerSalaDelCliente();

        switch (comando) {
            case "/crear":
                // Crear sala y cambiar estado
                ss.crear(cliente);
                break;
            case "/unirse":
                if (partes.length > 1) {
                    ss.unirse(partes[1]);
                    break;
                }
                cliente.salida().writeUTF("Uso: /unirse [nombreSala]");
                break;
            case "/ver":
                ss.ver();
                break;
            case "/salir":
                // Implementar lógica de salir si es necesario
                cliente.salida().writeUTF("Comando salir no implementado.");
                break;
            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual);
                break;
            default:
                cliente.salida().writeUTF("Comando desconocido.");
        }
    }

}
