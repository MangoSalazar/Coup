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

        Sala salaActual = ss.obtenerSalaDelCliente();
        boolean enPartida = (salaActual != null && salaActual.estaEnPartida());

        // Si empieza con '/' es comando.
        // Si está en partida y el mensaje empieza con un número, también es comando.
        if (mensaje.startsWith("/") || (enPartida && Character.isDigit(mensaje.trim().charAt(0)))) {
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

        // Si es numérico, delegamos directo a ServicioPartida para que lo interprete
        if (Character.isDigit(comando.charAt(0))) {
            new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
            return;
        }

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

            // Comandos clásicos de texto (por compatibilidad)
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
                cliente.salida().writeUTF("Comando desconocido. Escribe /menu.");
        }
    }

    public static void enviarBienvenida(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "=========================================\n" +
                "      BIENVENIDO A COUP - LOBBY\n" +
                "=========================================\n" +
                " /crear             -> Crea sala.\n" +
                " /unirse [nombre]   -> Únete a sala.\n" +
                " /iniciar           -> Inicia partida.\n" +
                "=========================================\n";
        cliente.salida().writeUTF(menu);
    }
}