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
        // VALIDACIÓN: No permitir mensajes vacíos o espacios en blanco
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

        // Si está en sala, solo envía a la sala. Si no, al lobby.
        if (salaActual != null) {
            String formato = "[" + cliente.getId() + "]: " + mensaje;
            salaActual.broadcast(formato, cliente);
            return;
        }

        String formato = "[LOBBY - " + cliente.getId() + "]: " + mensaje;
        ServidorMulti.broadcastGlobal(formato, cliente);
    }

    private void procesarComando(String mensaje) throws IOException {
        String[] partes = mensaje.trim().split("\\s+"); // Mejor manejo de espacios
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
                // Lógica simple para salir de sala si está en una (pendiente implementar lógica completa en ServicioSala)
                if (salaActual != null) {
                    cliente.salida().writeUTF("Comando salir no implementado completamente.");
                } else {
                    cliente.salida().writeUTF("No estás en ninguna sala.");
                }
                break;
            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual);
                break;
            // Agregamos comandos de ayuda para ver el menú nuevamente
            case "/menu":
            case "/ayuda":
            case "/comandos":
                enviarBienvenida(cliente);
                break;
            default:
                cliente.salida().writeUTF("Comando desconocido. Escribe /ayuda para ver los comandos.");
        }
    }

    // NUEVO MÉTODO: Mensaje de bienvenida con lista de comandos
    public static void enviarBienvenida(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "=========================================\n" +
                "      BIENVENIDO A COUP - LOBBY\n" +
                "=========================================\n" +
                "Comandos disponibles:\n" +
                " /crear             -> Crea una sala nueva con tu ID.\n" +
                " /unirse [nombre]   -> Únete a una sala existente.\n" +
                " /ver               -> Ver lista de salas disponibles.\n" +
                " /iniciar           -> (Solo Admin) Inicia la partida en la sala.\n" +
                " /salir             -> Salir de la sala actual.\n" +
                "=========================================\n";
        cliente.salida().writeUTF(menu);
    }
}