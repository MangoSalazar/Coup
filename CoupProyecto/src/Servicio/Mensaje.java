package Servicio;

import Dominio.Sala;
import Servidor.ServidorMulti;
import Servidor.UnCliente;
import java.io.IOException;

public class Mensaje {

    private UnCliente cliente;
    private ServicioSala sSala;

    public Mensaje(UnCliente cliente, ServicioSala ss) {
        this.cliente = cliente;
        this.sSala = ss;
    }

    public void manejarEntrada(String mensaje) throws IOException {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return;
        }

        Sala salaActual = sSala.obtenerSalaDelCliente();
        boolean enPartida = (salaActual != null && salaActual.estaEnPartida());

        if (mensaje.startsWith("/") || (enPartida && Character.isDigit(mensaje.trim().charAt(0)))) {
            procesarComando(mensaje);
            return;
        }
        procesarChat(mensaje);
    }

    private void procesarChat(String mensaje) {
        Sala salaActual = sSala.obtenerSalaDelCliente();
        if (salaActual != null) {
            salaActual.broadcast("[" + cliente.getId() + "]: " + mensaje, cliente);
            return;
        }
        ServidorMulti.broadcastGlobal("[LOBBY - " + cliente.getId() + "]: " + mensaje, cliente);
    }

    private void procesarComando(String mensaje) throws IOException {
        String[] partes = mensaje.trim().split("\\s+");
        String comando = partes[0];
        Sala salaActual = sSala.obtenerSalaDelCliente();

        if (Character.isDigit(comando.charAt(0))) {
            new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
            return;
        }

        switch (comando) {
            case "/crear":
                sSala.crear(cliente);
                break;
            case "/unirse":
                if (partes.length > 1) {
                    sSala.unirse(partes[1]);
                    break;
                }
                cliente.salida().writeUTF("Uso: /unirse [nombreSala]");
                break;
            case "/expulsar":
                if (partes.length > 1) {
                    sSala.expulsar(partes[1],salaActual);
                    break;
                }
                cliente.salida().writeUTF("Uso: /expulsar [nombreJugador]");
                break;
            case "/ver":
                sSala.ver();
                break;
            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual);
                break;

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

            case "/salir":
                if (salaActual.estaEnPartida()) {
                    new ServicioPartida(cliente).manejarSalida(salaActual);
                    break;
                }
                sSala.salir(salaActual);
                break;

            case "/menu":
                enviarBienvenida(cliente);
                break;
            default:
                cliente.salida().writeUTF("Comando desconocido. Escribe /menu.");
        }
    }

    public static void enviarBienvenida(UnCliente cliente) throws IOException {
        cliente.salida().writeUTF("\n=== COUP LOBBY ===\n /crear, /unirse [sala], /iniciar, /salir\n");
    }
}
