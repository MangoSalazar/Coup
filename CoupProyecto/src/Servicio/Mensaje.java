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
        if (mensaje == null || mensaje.trim().isEmpty()) return;

        Sala salaActual = ss.obtenerSalaDelCliente();
        boolean enPartida = (salaActual != null && salaActual.estaEnPartida());

        if (mensaje.startsWith("/") || (enPartida && Character.isDigit(mensaje.trim().charAt(0)))) {
            procesarComando(mensaje);
            return;
        }
        procesarChat(mensaje);
    }

    private void procesarChat(String mensaje) {
        Sala salaActual = ss.obtenerSalaDelCliente();
        if (salaActual != null) {
            salaActual.broadcast("[" + cliente.getId() + "]: " + mensaje, cliente);
        } else {
            ServidorMulti.broadcastGlobal("[LOBBY - " + cliente.getId() + "]: " + mensaje, cliente);
        }
    }

    private void procesarComando(String mensaje) throws IOException {
        String[] partes = mensaje.trim().split("\\s+");
        String comando = partes[0];
        Sala salaActual = ss.obtenerSalaDelCliente();

        if (Character.isDigit(comando.charAt(0))) {
            new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
            return;
        }

        switch (comando) {
            case "/crear": ss.crear(cliente); break;
            case "/unirse":
                if (partes.length > 1) ss.unirse(partes[1]);
                else cliente.salida().writeUTF("Uso: /unirse [nombreSala]");
                break;
            case "/ver": ss.ver(); break;
            case "/iniciar": new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual); break;

            case "/ingresos":
            case "/ayuda":
            case "/golpe":
            case "/impuestos":
            case "/asesinar":
            case "/extorsionar":
            case "/cambio":
            case "/revelar":
            case "/seleccionar":
            case "/desafiar":
            case "/permitir":
            case "/bloquear":
                new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
                break;

            case "/menu": enviarBienvenida(cliente); break;
            default: cliente.salida().writeUTF("Comando desconocido. Escribe /menu.");
        }
    }

    public static void enviarBienvenida(UnCliente cliente) throws IOException {
        cliente.salida().writeUTF("\n=== COUP LOBBY ===\n /crear, /unirse [sala], /iniciar\n");
    }
}