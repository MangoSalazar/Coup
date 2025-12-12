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
            salaActual.broadcast("["+salaActual.obtenerNombre()+" - "+ cliente.getId() + "]: " + mensaje, cliente);
            return;
        }
        ServidorMulti.broadcastGlobal("[LOBBY - " + cliente.getId() + "]: " + mensaje, cliente);
    }

    private void procesarComando(String mensaje) throws IOException {
        String[] partes = mensaje.trim().split("\\s+");
        String comando = partes[0];
<<<<<<< HEAD
        Sala salaActual = ss.obtenerSalaDelCliente();
        ServicioSesion servicioS = cliente.getServicioSesion();
=======
        Sala salaActual = sSala.obtenerSalaDelCliente();

        if (Character.isDigit(comando.charAt(0))) {
            new ServicioPartida(cliente).manejarAccionDeJuego(mensaje, salaActual);
            return;
        }
>>>>>>> partida

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
<<<<<<< HEAD
            case "/ver":
                ss.ver();
                break;
            case "/salir":
                if (salaActual != null) {
                    ss.salir();
                    cliente.salida().writeUTF("saliste de sala "+salaActual.obtenerNombre());
                    break;
                }
                cliente.salida().writeUTF("cerrando sesion...");
                servicioS.cerrarSesion();
=======
            case "/expulsar":
                if (partes.length > 1) {
                    sSala.expulsar(partes[1],salaActual);
                    break;
                }
                cliente.salida().writeUTF("Uso: /expulsar [nombreJugador]");
                break;
            case "/ver":
                sSala.ver();
>>>>>>> partida
                break;
            case "/iniciar":
                new ServicioPartida(cliente).manejarInicioPartida(cliente, salaActual);
                break;

<<<<<<< HEAD
            //acciones de juego
=======
>>>>>>> partida
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
<<<<<<< HEAD
        String menu = "\n"
                + "=========================================\n"
                + "      BIENVENIDO A COUP - LOBBY\n"
                + "=========================================\n"
                + "Comandos de Sala:\n"
                + " /crear             -> Crea una sala nueva.\n"
                + " /unirse [nombre]   -> Únete a una sala.\n"
                + " /ver               -> Ver salas disponibles.\n"
                + " /iniciar           -> (Admin) Inicia la partida.\n"
                + " /salir           -> si no estas en una sala te cierra sesion, sino te saca de ella\n"
                + "\n"
                + "Comandos de Juego (Solo en partida):\n"
                + " /ingresos, /ayuda, /golpe\n"
                + " /impuestos, /asesinar, /extorsionar, /cambio\n"
                + "=========================================\n";
        cliente.salida().writeUTF(menu);
=======
        cliente.salida().writeUTF("\n=== COUP LOBBY ===\n /crear, /unirse [sala], /ver, /expulsar [nombreCliente], /iniciar, /salir\n");
>>>>>>> partida
    }
}
