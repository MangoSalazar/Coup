package Servicio;

import Coup.Carta;
import Coup.Jugador;
import Dominio.Partida;
import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class ServicioPartida {

    private UnCliente cliente;

    public ServicioPartida(UnCliente cliente) {
        this.cliente = cliente;
    }

    public void manejarAccionDeJuego(String comandoOriginal, Sala sala) throws IOException {
        if (sala == null || !sala.estaEnPartida()) {
            cliente.salida().writeUTF("Error: No hay partida activa.");
            return;
        }

        Partida partida = sala.getPartida();

        // --- TRADUCCIÓN DE NÚMEROS A COMANDOS (SEGÚN CONTEXTO) ---
        String comando = interpretarNumero(comandoOriginal, partida);
        String[] partes = comando.split(" ");

        // 1. Revelar Cartas (Por muerte o pérdida)
        if (comando.startsWith("/revelar")) {
            manejarRevelacion(comando, sala, partida);
            return;
        }

        // 2. Embajador (Selección)
        if (comando.startsWith("/seleccionar")) {
            if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                partida.cancelarTemporizador();
            }
            manejarSeleccionEmbajador(comando, sala, partida);
            return;
        }

        // --- PAUSAS ---
        if (partida.getJugadorVictima() != null) {
            if (partida.getJugadorVictima().getCliente().equals(cliente)) {
                mostrarOpcionesRevelar(partida.getJugadorVictima()); // Recordar opciones
            } else {
                cliente.salida().writeUTF("Esperando a que " + partida.getJugadorVictima().getId() + " elija carta a perder.");
            }
            return;
        }

        if (partida.getJugadorIntercambio() != null) {
            if (partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                cliente.salida().writeUTF("Selecciona cartas (20s). Ej: '1 2'");
            } else {
                cliente.salida().writeUTF("Esperando al Embajador...");
            }
            return;
        }

        // 3. Desafíos
        if (comando.startsWith("/desafiar") || comando.startsWith("/permitir")) {
            partida.cancelarTemporizador();
            if (comando.startsWith("/desafiar")) manejarDesafio(sala, partida);
            else manejarPermiso(sala, partida);
            return;
        }

        if (partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("¡Hay una acción esperando! Usa: [1] Desafiar o [2] Permitir.");
            return;
        }

        // 4. Turno
        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno!");
            return;
        }

        partida.cancelarTemporizador();
        Jugador jugadorActual = partida.getJugador(cliente);

        if (partida.debeDarGolpe(jugadorActual) && !comando.startsWith("/golpe")) {
            cliente.salida().writeUTF("¡10+ monedas! Debes usar: [7] Golpe [jugador]");
            iniciarTimerTurno(partida, sala, jugadorActual);
            return;
        }

        String accion = partes[0];

        switch (accion) {
            case "/ingresos":
                partida.accionIngresos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " seleccionó: [1] INGRESOS (+1).");
                avanzarTurno(partida, sala);
                break;
            case "/ayuda":
                partida.accionAyudaExterior(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " seleccionó: [2] AYUDA EXTERIOR (+2).");
                avanzarTurno(partida, sala);
                break;
            case "/impuestos":
                partida.setAccionPendiente("IMPUESTOS", jugadorActual, null, "DUQUE");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "[3] IMPUESTOS", "DUQUE", null);
                break;
            case "/asesinar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "ASESINATO")) {
                    if (jugadorActual.getMonedas() < 3) {
                        cliente.salida().writeUTF("Necesitas 3 monedas.");
                        iniciarTimerTurno(partida, sala, jugadorActual);
                        return;
                    }
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    partida.setAccionPendiente("ASESINATO", jugadorActual, victima, "ASESINO");
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "[4] ASESINAR", "ASESINO", victima);
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            case "/extorsionar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "EXTORSIÓN")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    partida.setAccionPendiente("EXTORSIÓN", jugadorActual, victima, "CAPITAN");
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "[5] EXTORSIONAR", "CAPITAN", victima);
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            case "/cambio":
                partida.setAccionPendiente("CAMBIO", jugadorActual, null, "EMBAJADOR");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "[6] CAMBIO", "EMBAJADOR", null);
                break;
            case "/golpe":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "GOLPE")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    int res = partida.iniciarGolpe(jugadorActual, victima);
                    if (res == 0) {
                        cliente.salida().writeUTF("Faltan monedas (7).");
                        iniciarTimerTurno(partida, sala, jugadorActual);
                    } else if (res == 1) {
                        sala.broadcast("!!! " + victima.getId() + " recibió un [7] GOLPE DE ESTADO.");
                        solicitarCartaAPerder(victima);
                    } else {
                        sala.broadcast("!!! " + victima.getId() + " eliminado por GOLPE.");
                        if(!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
                        avanzarTurno(partida, sala);
                    }
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            default:
                cliente.salida().writeUTF("Acción inválida. Usa los números del menú.");
                iniciarTimerTurno(partida, sala, jugadorActual);
        }
    }

    // --- INTERPRETE DE NÚMEROS ---
    private String interpretarNumero(String input, Partida partida) {
        String[] partes = input.trim().split("\\s+");
        String primeraParte = partes[0];

        // Si no es dígito, asumimos que es comando de texto completo (/ingresos)
        if (!Character.isDigit(primeraParte.charAt(0))) return input;

        int opcion;
        try { opcion = Integer.parseInt(primeraParte); } catch(Exception e) { return input; }

        // Contexto: Víctima eligiendo carta (1 o 2)
        if (partida.getJugadorVictima() != null && partida.getJugadorVictima().getCliente().equals(cliente)) {
            Jugador yo = partida.getJugadorVictima();
            int index = opcion - 1;
            List<Carta> mano = yo.getMano();
            // Buscamos la carta no revelada en ese índice "visual"
            int contadorVisibles = 0;
            for (Carta c : mano) {
                if (!c.estaRevelada()) {
                    if (contadorVisibles == index) return "/revelar " + c.getRol().toString();
                    contadorVisibles++;
                }
            }
            return input; // Índice inválido
        }

        // Contexto: Embajador eligiendo cartas (ej: "1 2")
        if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().getCliente().equals(cliente)) {
            // Reconstruimos comando: "/seleccionar c1 c2"
            // Necesitamos los nombres de las cartas basados en los índices
            Jugador yo = partida.getJugadorIntercambio();
            StringBuilder sb = new StringBuilder("/seleccionar");
            List<Carta> mano = yo.getMano(); // Incluye las nuevas

            // Recorremos todos los números enviados (ej input: "1 3")
            for (String p : partes) {
                try {
                    int idx = Integer.parseInt(p) - 1;
                    if (idx >= 0 && idx < mano.size()) {
                        // En embajador, todas están "en mano" (incluso las del mazo se agregaron temp)
                        // pero filtramos reveladas por si acaso (aunque no deberia haber)
                        sb.append(" ").append(mano.get(idx).getRol().toString());
                    }
                } catch(Exception ignored) {}
            }
            return sb.toString();
        }

        // Contexto: Desafío pendiente
        if (partida.hayAccionPendiente()) {
            if (opcion == 1) return "/desafiar";
            if (opcion == 2) return "/permitir";
            return input;
        }

        // Contexto: Turno Principal
        if (partida.esTurnoDe(cliente)) {
            String arg = (partes.length > 1) ? partes[1] : "";
            switch (opcion) {
                case 1: return "/ingresos";
                case 2: return "/ayuda";
                case 3: return "/impuestos";
                case 4: return "/asesinar " + arg;
                case 5: return "/extorsionar " + arg;
                case 6: return "/cambio";
                case 7: return "/golpe " + arg;
            }
        }
        return input;
    }

    // --- TIMERS ---
    private void iniciarTimerTurno(Partida partida, Sala sala, Jugador jugador) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (jugador.getMonedas() >= 10) {
                        Jugador victima = partida.obtenerVictimaAleatoria(jugador);
                        if (victima != null) {
                            sala.broadcast("\n⌛ TIEMPO AGOTADO (10+ monedas).");
                            sala.broadcast(">> GOLPE AUTOMÁTICO a " + victima.getId());
                            int res = partida.iniciarGolpe(jugador, victima);
                            if (res == 1) {
                                solicitarCartaAPerder(victima);
                                partida.setJugadorVictima(victima);
                            } else {
                                sala.broadcast("!!! " + victima.getId() + " ELIMINADO.");
                                avanzarTurno(partida, sala);
                            }
                        } else { avanzarTurno(partida, sala); }
                    } else {
                        sala.broadcast("\n⌛ TIEMPO AGOTADO. Se aplican INGRESOS.");
                        partida.accionIngresos(jugador);
                        avanzarTurno(partida, sala);
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
        }, 10000);
    }

    private void iniciarTimerDesafio(Partida partida, Sala sala) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    sala.broadcast("\n⌛ NADIE DESAFIÓ. Acción procede.");
                    ejecutarAccionPendiente(sala, partida);
                    if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                        avanzarTurno(partida, sala);
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
        }, 10000);
    }

    private void iniciarTimerSeleccion(Partida partida, Sala sala, Jugador jugador) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    sala.broadcast("\n⌛ TIEMPO SELECCIÓN AGOTADO.");
                    partida.cancelarIntercambio();
                    avanzarTurno(partida, sala);
                } catch (IOException e) { e.printStackTrace(); }
            }
        }, 20000);
    }

    // --- MENUS Y RESPUESTAS ---

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "--- ACCIONES (Escribe el número) ---\n" +
                " [1] Ingresos (+1)\n" +
                " [2] Ayuda Exterior (+2)\n" +
                " [3] Impuestos (+3, Duque)\n" +
                " [4] Asesinar [nombre] (3$, Asesino)\n" +
                " [5] Extorsionar [nombre] (Capitán)\n" +
                " [6] Cambio (Embajador)\n" +
                " [7] Golpe [nombre] (7$)\n" +
                "----------------\n";
        cliente.salida().writeUTF(menu);
    }

    private void solicitarCartaAPerder(Jugador victima) throws IOException {
        mostrarOpcionesRevelar(victima);
        victima.getCliente().salida().writeUTF("PERDISTE UNA CARTA. Escribe el número ([1], [2]...) para revelarla.");
    }

    private void mostrarOpcionesRevelar(Jugador j) throws IOException {
        StringBuilder sb = new StringBuilder("\n--- TUS CARTAS ---\n");
        int i = 1;
        for (Carta c : j.getMano()) {
            if (!c.estaRevelada()) {
                sb.append(" [").append(i).append("] ").append(c.verNombre()).append("\n");
                i++;
            }
        }
        j.getCliente().salida().writeUTF(sb.toString());
    }

    private void mostrarOpcionesEmbajador(Jugador j) throws IOException {
        StringBuilder sb = new StringBuilder("\n--- ELIGE CARTAS A CONSERVAR (Ej: '1 2') ---\n");
        int i = 1;
        for (Carta c : j.getMano()) {
            sb.append(" [").append(i).append("] ").append(c.verNombre()).append("\n");
            i++;
        }
        j.getCliente().salida().writeUTF(sb.toString());
    }

    // --- FLUJO ---

    private void anunciarAccionDesafiable(Sala sala, Partida partida, Jugador actor, String accion, String carta, Jugador victima) throws IOException {
        sala.broadcast("\n------------------------------------------------");
        sala.broadcast("⚠️  " + actor.getId() + " quiere usar " + accion);
        sala.broadcast("Dice ser: [" + carta + "]" + (victima != null ? " contra " + victima.getId() : ""));
        sala.broadcast("¿Qué hacen los demás? [1] Desafiar  |  [2] Permitir");
        sala.broadcast("------------------------------------------------\n");
        iniciarTimerDesafio(partida, sala);
    }

    private void manejarDesafio(Sala sala, Partida partida) throws IOException {
        if (!partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("No hay acción pendiente.");
            return;
        }
        Jugador desafiante = partida.getJugador(cliente);
        Jugador actor = partida.getActorPendiente();

        if (desafiante.equals(actor)) {
            cliente.salida().writeUTF("No puedes desafiarte a ti mismo.");
            iniciarTimerDesafio(partida, sala);
            return;
        }

        String cartaRequerida = partida.getCartaRequeridaPendiente();
        sala.broadcast("\n!!! " + desafiante.getId() + " ha DESAFIADO a " + actor.getId() + " !!!");

        if (partida.tieneCarta(actor, cartaRequerida)) {
            sala.broadcast(">> " + actor.getId() + " TIENE " + cartaRequerida + ". ¡Es INOCENTE!");
            sala.broadcast("❌ " + desafiante.getId() + " pierde el desafío.");
            partida.cambiarCartaPorGanarDesafio(actor, cartaRequerida);
            ejecutarAccionPendiente(sala, partida);
            aplicarPenalizacion(sala, partida, desafiante);
        } else {
            sala.broadcast(">> " + actor.getId() + " NO TIENE " + cartaRequerida + ". ¡MINTIÓ!");
            sala.broadcast("❌ Acción cancelada.");
            partida.limpiarAccionPendiente();
            aplicarPenalizacion(sala, partida, actor);
        }
    }

    private void manejarPermiso(Sala sala, Partida partida) throws IOException {
        if (!partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("Nada que permitir.");
            return;
        }
        Jugador actor = partida.getActorPendiente();
        if (partida.getJugador(cliente).equals(actor)) {
            cliente.salida().writeUTF("Espera.");
            iniciarTimerDesafio(partida, sala);
            return;
        }
        sala.broadcast(cliente.getId() + " permitió la jugada.");
        ejecutarAccionPendiente(sala, partida);
        if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
            avanzarTurno(partida, sala);
        }
    }

    private void ejecutarAccionPendiente(Sala sala, Partida partida) throws IOException {
        String accion = partida.getAccionPendiente();
        Jugador actor = partida.getActorPendiente();
        Jugador victima = partida.getObjetivoPendiente();
        partida.limpiarAccionPendiente();
        if (accion == null) return;

        switch (accion) {
            case "IMPUESTOS":
                partida.accionImpuestos(actor);
                sala.broadcast(">> " + actor.getId() + " cobró IMPUESTOS (+3).");
                break;
            case "ASESINATO":
                int res = partida.iniciarAsesinato(actor, victima);
                if (res == 1) {
                    sala.broadcast("!!! " + victima.getId() + " ASESINADO.");
                    solicitarCartaAPerder(victima);
                } else if (res == 2) {
                    sala.broadcast("!!! " + victima.getId() + " ELIMINADO por Asesinato.");
                } else {
                    sala.broadcast("El objetivo ya no es válido.");
                }
                break;
            case "EXTORSIÓN":
                partida.accionExtorsion(actor, victima);
                sala.broadcast(">> " + actor.getId() + " extorsionó a " + victima.getId());
                break;
            case "CAMBIO":
                partida.iniciarEmbajador(actor);
                sala.broadcast(">> " + actor.getId() + " examina el mazo.");
                mostrarOpcionesEmbajador(actor);
                iniciarTimerSeleccion(partida, sala, actor);
                break;
        }
    }

    private void aplicarPenalizacion(Sala sala, Partida partida, Jugador perdedor) throws IOException {
        if (perdedor.getInfluenciaActiva() <= 1) {
            perdedor.perderInfluencia();
            sala.broadcast("☠ " + perdedor.getId() + " ELIMINADO.");
            if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                avanzarTurno(partida, sala);
            }
        } else {
            solicitarCartaAPerder(perdedor);
            partida.setJugadorVictima(perdedor);
        }
    }

    private void manejarRevelacion(String comando, Sala sala, Partida partida) throws IOException {
        Jugador victima = partida.getJugadorVictima();
        if (victima == null || !victima.getCliente().equals(cliente)) return;
        String[] partes = comando.split(" ");
        if (partes.length < 2) return;

        if (partida.concretarPerdida(victima, partes[1])) {
            sala.broadcast("☠ " + victima.getId() + " perdió: " + partes[1]);
            if (!victima.estaVivo()) sala.broadcast("☠ ELIMINADO: " + victima.getId());
            if (partida.getJugadorIntercambio() == null) avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Carta no válida.");
        }
    }

    private void manejarSeleccionEmbajador(String comando, Sala sala, Partida partida) throws IOException {
        Jugador actor = partida.getJugadorIntercambio();
        if (actor == null || !actor.getCliente().equals(cliente)) return;
        String[] partes = comando.split(" ");
        String c1 = (partes.length > 1) ? partes[1] : null;
        String c2 = (partes.length > 2) ? partes[2] : null;

        if (partida.concretarIntercambio(actor, c1, c2)) {
            sala.broadcast(">> " + actor.getId() + " terminó el cambio.");
            avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Error: Selección inválida.");
        }
    }

    private void avanzarTurno(Partida partida, Sala sala) throws IOException {
        Jugador ganador = partida.obtenerGanador();
        if (ganador != null) {
            sala.broadcast("\n*** ¡GANADOR: " + ganador.getId().toUpperCase() + "! ***");
            sala.setPartida(null);
            partida.cancelarTemporizador();
            return;
        }
        partida.siguienteTurno();
        Jugador siguiente = partida.obtenerJugadorTurno();
        sala.broadcast("------------------------------------------------");
        sala.broadcast("Turno de: " + siguiente.getId() + " | Monedas: " + siguiente.getMonedas());

        // Mostrar cartas en formato numérico para futura referencia
        mostrarOpcionesRevelar(siguiente); // Reusamos este método para mostrar cartas
        enviarMenuAcciones(siguiente.getCliente());
        iniciarTimerTurno(partida, sala, siguiente);
    }

    private boolean procesarAtaque(String[] partes, Sala sala, Partida partida, Jugador atacante, String accion) throws IOException {
        if (partes.length < 2) {
            cliente.salida().writeUTF("Falta objetivo (Ej: [4] Juan).");
            return false;
        }
        Jugador victima = obtenerVictima(partes, sala, partida);
        if (victima == null || !victima.estaVivo()) {
            cliente.salida().writeUTF("Jugador no válido.");
            return false;
        }
        if (victima.equals(atacante)) {
            cliente.salida().writeUTF("No puedes atacarte.");
            return false;
        }
        return true;
    }

    private Jugador obtenerVictima(String[] partes, Sala sala, Partida partida) {
        String nombreVictima = partes[1];
        for (UnCliente c : sala.obtenerIntegrantes()) {
            if (c.getId().equals(nombreVictima)) return partida.getJugador(c);
        }
        return null;
    }

    public void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null && sala.getAdministrador().equals(cliente)) {
            Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
            sala.setPartida(nuevaPartida);
            sala.broadcast(">>> ¡PARTIDA INICIADA! <<<");
            Jugador primer = nuevaPartida.obtenerJugadorTurno();
            sala.broadcast("Turno de: " + primer.getId());
            mostrarOpcionesRevelar(primer);
            enviarMenuAcciones(primer.getCliente());
            iniciarTimerTurno(nuevaPartida, sala, primer);
        }
    }
}