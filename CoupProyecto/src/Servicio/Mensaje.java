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
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return;
        }

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
        String[] partes = mensaje.trim().split("\\s+");
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
                cliente.salida().writeUTF("Uso: /unirse [nombreSala]");
                break;
            case "/ver":
                ss.ver();
                break;
            case "/salir":
                if (salaActual != null) {
                    cliente.salida().writeUTF("Comando salir no implementado completamente.");
                } else {
                    cliente.salida().writeUTF("No estás en ninguna sala.");
                }
                break;
            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual);
                break;

            // --- ACCIONES DE JUEGO ---
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
                new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
                break;

            case "/menu":
                enviarBienvenida(cliente);
                break;

            default:
                cliente.salida().writeUTF("Comando desconocido. Escribe /menu para ver los comandos.");
        }
    }

    public static void enviarBienvenida(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "=========================================\n" +
                "      BIENVENIDO A COUP - LOBBY\n" +
                "=========================================\n" +
                "Comandos de Sala:\n" +
                " /crear             -> Crea una sala nueva.\n" +
                " /unirse [nombre]   -> Únete a una sala.\n" +
                " /ver               -> Ver salas disponibles.\n" +
                " /iniciar           -> (Admin) Inicia la partida.\n" +
                "\n" +
                "Comandos de Juego:\n" +
                " /ingresos, /ayuda, /golpe\n" +
                " /impuestos, /asesinar, /extorsionar, /cambio\n" +
                " /desafiar, /permitir\n" +
                "=========================================\n";
        cliente.salida().writeUTF(menu);
    }
}