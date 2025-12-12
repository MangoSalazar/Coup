package Servicio;

import Coup.Carta;
import Coup.Jugador;
import Dominio.Partida;
import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

public class ServicioPartida {

    private UnCliente cliente;

    public ServicioPartida(UnCliente cliente) {
        this.cliente = cliente;
    }

    // --- MÉTODO RESTAURADO PARA /SALIR ---
    public void manejarSalida(Sala sala) throws IOException {
        Partida partida = sala.getPartida();
        Jugador jugador = partida.getJugador(cliente);

        if (jugador == null || !jugador.estaVivo()) {
            cliente.salida().writeUTF("Ya no estás jugando activamente.");
            return;
        }

        // 1. ELIMINAR AL JUGADOR (Revelar todas sus cartas)
        for (Carta c : jugador.getMano()) {
            c.revelar();
        }
        sala.broadcast("\n☠ " + cliente.getId() + " se ha RENDIDO (/salir) y ha sido ELIMINADO.");

        // 2. LIMPIAR ESTADOS PENDIENTES
        boolean eraSuTurno = partida.esTurnoDe(cliente);

        if (partida.getJugadorVictima() != null && partida.getJugadorVictima().equals(jugador)) {
            partida.setJugadorVictima(null);
            partida.setEjecucionAsesinatoPendiente(false);
        }

        if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().equals(jugador)) {
            partida.cancelarIntercambio();
        }

        if (partida.hayAccionPendiente()) {
            if (jugador.equals(partida.getActorPendiente()) || jugador.equals(partida.getObjetivoPendiente())) {
                sala.broadcast("⚠️ La acción pendiente se cancela por abandono.");
                partida.limpiarAccionPendiente();
                partida.cancelarTemporizador();

                if (!eraSuTurno) {
                    avanzarTurno(partida, sala);
                    return;
                }
            }
        }

        // 3. VERIFICAR GANADOR
        if (partida.obtenerGanador() != null) {
            avanzarTurno(partida, sala);
            return;
        }

        // 4. PASAR TURNO
        if (eraSuTurno) {
            partida.cancelarTemporizador();
            avanzarTurno(partida, sala);
        }
    }

    // --- LÓGICA PRINCIPAL ---
    public void manejarAccionDeJuego(String comandoOriginal, Sala sala) throws IOException {
        if (sala == null || !sala.estaEnPartida()) {
            cliente.salida().writeUTF("Error: No hay partida activa.");
            return;
        }

        Partida partida = sala.getPartida();
        String comando = interpretarNumero(comandoOriginal, partida);
        String[] partes = comando.split(" ");
        String accion = partes[0];

        // 1. PRIORIDADES
        if (accion.equals("/revelar")) {
            manejarRevelacion(comando, sala, partida);
            return;
        }

        if (accion.equals("/seleccionar")) {
            if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                partida.cancelarTemporizador();
            }
            manejarSeleccionEmbajador(comando, sala, partida);
            return;
        }

        // 2. PAUSAS
        if (partida.getJugadorVictima() != null) {
            if (partida.getJugadorVictima().getCliente().equals(cliente)) {
                mostrarEstadoJugador(partida.getJugadorVictima());
                cliente.salida().writeUTF("¡Debes perder una carta! Escribe el número [1] o [2].");
            } else {
                cliente.salida().writeUTF("Esperando a que " + partida.getJugadorVictima().getId() + " pierda una carta.");
            }
            return;
        }

        if (partida.getJugadorIntercambio() != null) {
            if (partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                cliente.salida().writeUTF("Estás cambiando cartas (20s). Usa: 1 2");
            } else {
                cliente.salida().writeUTF("Esperando al Embajador...");
            }
            return;
        }

        // 3. RESPUESTAS
        if (accion.equals("/desafiar") || accion.equals("/permitir") || accion.equals("/bloquear")) {
            partida.cancelarTemporizador();
            if (accion.equals("/desafiar")) manejarDesafio(sala, partida);
            else if (accion.equals("/bloquear")) manejarBloqueo(sala, partida, partes); // Pasamos partes para leer carta de bloqueo
            else manejarPermiso(sala, partida);
            return;
        }

        if (partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("¡Hay una acción esperando! Usa las opciones del menú.");
            return;
        }

        // 4. TURNO
        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno!");
            return;
        }

        partida.cancelarTemporizador();
        Jugador jugadorActual = partida.getJugador(cliente);

        if (partida.debeDarGolpe(jugadorActual) && !accion.equals("/golpe")) {
            cliente.salida().writeUTF("¡Tienes 10+ monedas! Debes usar: 7. /golpe");
            iniciarTimerTurno(partida, sala, jugadorActual);
            return;
        }

        switch (accion) {
            case "/ingresos":
                partida.accionIngresos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " tomó INGRESOS (+1).");
                avanzarTurno(partida, sala);
                break;
            case "/ayuda":
                partida.setAccionPendiente("AYUDA", jugadorActual, null, null);
                anunciarAccionDesafiable(sala, partida, jugadorActual, "AYUDA EXTERIOR", null, null);
                break;
            case "/impuestos":
                partida.setAccionPendiente("IMPUESTOS", jugadorActual, null, "DUQUE");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "3. /impuestos", "DUQUE", null);
                break;
            case "/asesinar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "ASESINATO")) {
                    if (jugadorActual.getMonedas() < 3) {
                        cliente.salida().writeUTF("Necesitas 3 monedas.");
                        iniciarTimerTurno(partida, sala, jugadorActual);
                        return;
                    }
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    partida.setAccionPendiente("ASESINATO", jugadorActual, victima, "ASESINA");
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "4. /asesinar", "ASESINA", victima);
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            case "/extorsionar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "EXTORSIÓN")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    partida.setAccionPendiente("EXTORSIÓN", jugadorActual, victima, "CAPITAN");
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "5. /extorsionar", "CAPITAN", victima);
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            case "/cambio":
                partida.setAccionPendiente("CAMBIO", jugadorActual, null, "EMBAJADOR");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "6. /cambio", "EMBAJADOR", null);
                break;
            case "/golpe":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "GOLPE")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    int res = partida.iniciarGolpe(jugadorActual, victima);
                    if (res == 0) {
                        cliente.salida().writeUTF("Faltan monedas (7).");
                        iniciarTimerTurno(partida, sala, jugadorActual);
                    } else if (res == 1) {
                        sala.broadcast("!!! " + victima.getId() + " recibió un GOLPE DE ESTADO.");
                        solicitarCartaAPerder(victima);
                    } else {
                        sala.broadcast("!!! " + victima.getId() + " eliminado por GOLPE.");
                        if(!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
                        avanzarTurno(partida, sala);
                    }
                } else { iniciarTimerTurno(partida, sala, jugadorActual); }
                break;
            default:
                cliente.salida().writeUTF("Opción inválida.");
                iniciarTimerTurno(partida, sala, jugadorActual);
        }
    }

    private String interpretarNumero(String input, Partida partida) {
        String[] partes = input.trim().split("\\s+");
        String primeraParte = partes[0];
        if (!Character.isDigit(primeraParte.charAt(0))) return input;

        int opcion;
        try { opcion = Integer.parseInt(primeraParte); } catch(Exception e) { return input; }

        if (partida.getJugadorVictima() != null && partida.getJugadorVictima().getCliente().equals(cliente)) {
            Jugador yo = partida.getJugadorVictima();
            int index = opcion - 1;
            int contador = 0;
            for (Carta c : yo.getMano()) {
                if (!c.estaRevelada()) {
                    if (contador == index) return "/revelar " + c.getRol().toString();
                    contador++;
                }
            }
            return input;
        }

        if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().getCliente().equals(cliente)) {
            Jugador yo = partida.getJugadorIntercambio();
            StringBuilder sb = new StringBuilder("/seleccionar");
            List<Carta> mano = yo.getMano();
            for (String p : partes) {
                try {
                    int idx = Integer.parseInt(p) - 1;
                    if (idx >= 0 && idx < mano.size()) sb.append(" ").append(mano.get(idx).getRol().toString());
                } catch(Exception ignored) {}
            }
            return sb.toString();
        }

        // RESPUESTAS
        if (partida.hayAccionPendiente()) {
            boolean esAsesinato = "ASESINATO".equals(partida.getAccionPendiente());
            boolean esAyuda = "AYUDA".equals(partida.getAccionPendiente());
            boolean esExtorsion = "EXTORSIÓN".equals(partida.getAccionPendiente());

            boolean soyObjetivo = partida.getObjetivoPendiente() != null && partida.getObjetivoPendiente().getCliente().equals(cliente);
            boolean soyEspectador = !partida.getActorPendiente().getCliente().equals(cliente);

            if (opcion == 3 && esAsesinato && soyObjetivo) return "/bloquear CONDESA";
            if (opcion == 3 && esAyuda && soyEspectador) return "/bloquear DUQUE";
            if (esExtorsion && soyObjetivo) {
                if (opcion == 3) return "/bloquear CAPITAN";
                if (opcion == 4) return "/bloquear EMBAJADOR";
            }

            if (opcion == 1) return "/desafiar";
            if (opcion == 2) return "/permitir";
            return input;
        }

        // TURNO
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

    private void manejarBloqueo(Sala sala, Partida partida, String[] partes) throws IOException {
        String accionActual = partida.getAccionPendiente();
        if (accionActual == null || (!"ASESINATO".equals(accionActual) && !"AYUDA".equals(accionActual) && !"EXTORSIÓN".equals(accionActual))) {
            cliente.salida().writeUTF("No puedes bloquear ahora.");
            return;
        }

        Jugador bloqueador = partida.getJugador(cliente);
        Jugador actorOriginal = partida.getActorPendiente();

        String cartaBloqueo = "CONDESA";
        String nombreAccionBloqueo = "BLOQUEO_CONDESA";

        if ("AYUDA".equals(accionActual)) {
            cartaBloqueo = "DUQUE";
            nombreAccionBloqueo = "BLOQUEO_AYUDA";
        } else if ("EXTORSIÓN".equals(accionActual)) {
            if (partes.length > 1) cartaBloqueo = partes[1];
            else cartaBloqueo = "CAPITAN";
            nombreAccionBloqueo = "BLOQUEO_EXTORSION";
        }

        sala.broadcast("\n✋ " + bloqueador.getId() + " bloquea con " + cartaBloqueo + ".");
        partida.setAccionPendiente(nombreAccionBloqueo, bloqueador, actorOriginal, cartaBloqueo);
        sala.broadcast("¿" + actorOriginal.getId() + " le cree? [1] Desafiar | [2] Permitir");
        iniciarTimerDesafio(partida, sala);
    }

    private void manejarDesafio(Sala sala, Partida partida) throws IOException {
        if (!partida.hayAccionPendiente()) return;
        Jugador desafiante = partida.getJugador(cliente);
        Jugador actor = partida.getActorPendiente();

        // Objetivo real de la acción original (necesario si falla bloqueo de extorsión/ayuda)
        Jugador actorOriginal = partida.getObjetivoPendiente();

        if (desafiante.equals(actor)) {
            cliente.salida().writeUTF("No puedes desafiarte.");
            iniciarTimerDesafio(partida, sala);
            return;
        }

        String cartaRequerida = partida.getCartaRequeridaPendiente();
        sala.broadcast("\n!!! " + desafiante.getId() + " DESAFÍA a " + actor.getId() + " !!!");

        if (partida.tieneCarta(actor, cartaRequerida)) {
            sala.broadcast(">> " + actor.getId() + " ES INOCENTE (Tiene " + cartaRequerida + ").");
            partida.cambiarCartaPorGanarDesafio(actor, cartaRequerida);

            if (partida.getAccionPendiente().startsWith("BLOQUEO")) {
                sala.broadcast("🛡️ Bloqueo exitoso. Acción original cancelada.");
                aplicarPenalizacion(sala, partida, desafiante);
            } else {
                // Si la VÍCTIMA desafió al ASESINO y perdió -> Doble Muerte
                if ("ASESINATO".equals(partida.getAccionPendiente())
                        && desafiante.equals(partida.getObjetivoPendiente())
                        && partida.contarCartasVivas(desafiante) > 1) {
                    partida.setEjecucionAsesinatoPendiente(true);
                }
                ejecutarAccionPendiente(sala, partida);
                aplicarPenalizacion(sala, partida, desafiante);
            }
        } else {
            sala.broadcast(">> " + actor.getId() + " MINTIÓ. Acción cancelada.");
            String tipoBloqueo = partida.getAccionPendiente();
            boolean eraBloqueo = tipoBloqueo.startsWith("BLOQUEO");

            partida.limpiarAccionPendiente();
            aplicarPenalizacion(sala, partida, actor);

            // Si el bloqueo falló, se ejecuta la acción original (si el culpable sigue vivo)
            if (eraBloqueo && actor.estaVivo()) {
                if ("BLOQUEO_CONDESA".equals(tipoBloqueo)) {
                    sala.broadcast("🔪 Bloqueo fallido: ASESINATO procede.");
                    partida.setEjecucionAsesinatoPendiente(true);
                } else if ("BLOQUEO_EXTORSION".equals(tipoBloqueo)) {
                    sala.broadcast("💰 Bloqueo fallido: EXTORSIÓN procede.");
                    partida.accionExtorsion(actorOriginal, actor);
                } else if ("BLOQUEO_AYUDA".equals(tipoBloqueo)) {
                    sala.broadcast("💰 Bloqueo fallido: AYUDA procede.");
                    partida.accionAyudaExterior(actorOriginal);
                }
            }
        }
    }

    private void manejarPermiso(Sala sala, Partida partida) throws IOException {
        if (!partida.hayAccionPendiente()) return;
        Jugador actor = partida.getActorPendiente();
        if (partida.getJugador(cliente).equals(actor)) {
            cliente.salida().writeUTF("Espera.");
            iniciarTimerDesafio(partida, sala);
            return;
        }
        sala.broadcast(cliente.getId() + " permite la jugada.");
        if (partida.getAccionPendiente().startsWith("BLOQUEO")) {
            sala.broadcast("🛡️ Bloqueo aceptado. Acción original cancelada.");
            partida.limpiarAccionPendiente();
            avanzarTurno(partida, sala);
        } else {
            ejecutarAccionPendiente(sala, partida);
            if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                avanzarTurno(partida, sala);
            }
        }
    }

    private void ejecutarAccionPendiente(Sala sala, Partida partida) throws IOException {
        String accion = partida.getAccionPendiente();
        Jugador actor = partida.getActorPendiente();
        Jugador victima = partida.getObjetivoPendiente();
        partida.limpiarAccionPendiente();
        if (accion == null) return;

        switch (accion) {
            case "AYUDA":
                partida.accionAyudaExterior(actor);
                sala.broadcast(">> " + actor.getId() + " recibió AYUDA EXTERIOR (+2).");
                break;
            case "IMPUESTOS":
                partida.accionImpuestos(actor);
                sala.broadcast(">> " + actor.getId() + " cobró IMPUESTOS (+3).");
                break;
            case "ASESINATO":
                int res = partida.iniciarAsesinato(actor, victima);
                if (res == 1) {
                    sala.broadcast("!!! " + victima.getId() + " ha sido ASESINADO.");
                    solicitarCartaAPerder(victima);
                } else if (res == 2) {
                    sala.broadcast("!!! " + victima.getId() + " ELIMINADO por Asesinato.");
                }
                break;
            case "EXTORSIÓN":
                partida.accionExtorsion(actor, victima);
                sala.broadcast(">> " + actor.getId() + " extorsionó a " + victima.getId());
                break;
            case "CAMBIO":
                partida.iniciarEmbajador(actor);
                sala.broadcast(">> " + actor.getId() + " examina mazo.");
                mostrarOpcionesEmbajador(actor);
                iniciarTimerSeleccion(partida, sala, actor);
                break;
        }
    }

    private void aplicarPenalizacion(Sala sala, Partida partida, Jugador perdedor) throws IOException {
        if (partida.contarCartasVivas(perdedor) <= 1) {
            String ultimaCarta = null;
            for(Carta c : perdedor.getMano()) if(!c.estaRevelada()) ultimaCarta = c.getRol().toString();

            if (ultimaCarta != null) {
                partida.concretarPerdida(perdedor, ultimaCarta);
                sala.broadcast("☠ " + perdedor.getId() + " pierde su última carta: " + ultimaCarta);
            }

            sala.broadcast("☠ " + perdedor.getId() + " ha sido ELIMINADO.");
            partida.setEjecucionAsesinatoPendiente(false);
            partida.setJugadorVictima(null);

            if (partida.getJugadorIntercambio() == null) {
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
        if (partes.length < 2) {
            cliente.salida().writeUTF("Uso: /revelar [NombreCarta]");
            return;
        }

        if (partida.concretarPerdida(victima, partes[1])) {
            sala.broadcast("☠ " + victima.getId() + " perdió: " + partes[1]);

            if (victima.estaVivo() && partida.isEjecucionAsesinatoPendiente()) {
                partida.setEjecucionAsesinatoPendiente(false);
                sala.broadcast("🔪 Doble Muerte: Aplicando ASESINATO pendiente.");
                aplicarPenalizacion(sala, partida, victima);
                return;
            }

            if (!victima.estaVivo()) sala.broadcast("☠ ELIMINADO: " + victima.getId());
            if (partida.getJugadorIntercambio() == null) avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Carta no válida.");
        }
    }

    private void avanzarTurno(Partida partida, Sala sala) throws IOException {
        Jugador ganador = partida.obtenerGanador();
        if (ganador != null) {
            sala.broadcast("\n*** ¡GANADOR: " + ganador.getId().toUpperCase() + "! ***");
            sala.broadcast(">>> Regresando al LOBBY en 10 segundos... <<<");
            partida.cancelarTemporizador();
            new java.util.Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        sala.setPartida(null);
                        sala.broadcast("\n=== HAS REGRESADO AL LOBBY ===");
                        for (UnCliente c : sala.obtenerIntegrantes()) Mensaje.enviarBienvenida(c);
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }, 10000);
            return;
        }
        partida.siguienteTurno();
        Jugador siguiente = partida.obtenerJugadorTurno();
        sala.broadcast("------------------------------------------------");
        sala.broadcast("Turno de: " + siguiente.getId() + " | Monedas: " + siguiente.getMonedas());
        mostrarEstadoJugador(siguiente);
        enviarMenuAcciones(siguiente.getCliente());
        iniciarTimerTurno(partida, sala, siguiente);
    }

    private void iniciarTimerTurno(Partida partida, Sala sala, Jugador jugador) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (jugador.getMonedas() >= 10) {
                        Jugador victima = partida.obtenerVictimaAleatoria(jugador);
                        if (victima != null) {
                            sala.broadcast("\n⌛ TIEMPO (10+) -> GOLPE AUTOMÁTICO a " + victima.getId());
                            if (partida.iniciarGolpe(jugador, victima) == 1) {
                                solicitarCartaAPerder(victima);
                                partida.setJugadorVictima(victima);
                            } else {
                                sala.broadcast("!!! " + victima.getId() + " ELIMINADO.");
                                avanzarTurno(partida, sala);
                            }
                        } else avanzarTurno(partida, sala);
                    } else {
                        sala.broadcast("\n⌛ TIEMPO -> INGRESOS.");
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
                    sala.broadcast("\n⌛ TIEMPO -> Acción procede.");
                    if (partida.getAccionPendiente().startsWith("BLOQUEO")) {
                        sala.broadcast("🛡️ Bloqueo efectivo por tiempo.");
                        partida.limpiarAccionPendiente();
                        avanzarTurno(partida, sala);
                    } else {
                        ejecutarAccionPendiente(sala, partida);
                        if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                            avanzarTurno(partida, sala);
                        }
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
                    sala.broadcast("\n⌛ TIEMPO SELECCIÓN -> Cancelado.");
                    partida.cancelarIntercambio();
                    avanzarTurno(partida, sala);
                } catch (IOException e) { e.printStackTrace(); }
            }
        }, 20000);
    }

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "--- ACCIONES (Escribe el número) ---\n" +
                " [1] Ingresos (+1)\n" +
                " [2] Ayuda Exterior (+2)\n" +
                " [3] Impuestos (+3, Duque)\n" +
                " [4] Asesinar [jug] (3$, Asesino)\n" +
                " [5] Extorsionar [jug] (Capitán)\n" +
                " [6] Cambio (Embajador)\n" +
                " [7] Golpe [jug] (7$)\n" +
                "----------------\n";
        cliente.salida().writeUTF(menu);
    }

    private void mostrarEstadoJugador(Jugador j) throws IOException {
        StringBuilder sb = new StringBuilder("Tus Cartas: ");
        int i = 1;
        for (Carta c : j.getMano()) {
            if (!c.estaRevelada()) {
                sb.append("[").append(i).append("] ").append(c.verNombre()).append(" ");
                i++;
            }
        }
        j.getCliente().salida().writeUTF(sb.toString());
    }

    private void mostrarOpcionesEmbajador(Jugador j) throws IOException {
        mostrarEstadoJugador(j);
        j.getCliente().salida().writeUTF("Usa: '1 2' para quedarte con esas cartas.");
    }

    private void solicitarCartaAPerder(Jugador victima) throws IOException {
        mostrarEstadoJugador(victima);
        victima.getCliente().salida().writeUTF("PERDISTE UNA CARTA. Escribe el número [1], [2]...");
    }

    private void manejarSeleccionEmbajador(String comando, Sala sala, Partida partida) throws IOException {
        Jugador actor = partida.getJugadorIntercambio();
        if (actor == null || !actor.getCliente().equals(cliente)) return;
        String[] partes = comando.split(" ");
        String c1 = (partes.length > 1) ? partes[1] : null;
        String c2 = (partes.length > 2) ? partes[2] : null;
        if (partida.concretarIntercambio(actor, c1, c2)) {
            sala.broadcast(">> Cambio terminado.");
            avanzarTurno(partida, sala);
        } else cliente.salida().writeUTF("Error en selección.");
    }

    private boolean procesarAtaque(String[] partes, Sala sala, Partida partida, Jugador atacante, String accion) throws IOException {
        if (partes.length < 2) {
            cliente.salida().writeUTF("Falta objetivo (Ej: '4 Juan').");
            return false;
        }
        Jugador victima = obtenerVictima(partes, sala, partida);
        if (victima == null || !victima.estaVivo()) {
            cliente.salida().writeUTF("Objetivo inválido.");
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

    private void anunciarAccionDesafiable(Sala sala, Partida partida, Jugador actor, String accion, String carta, Jugador victima) throws IOException {
        sala.broadcast("\n------------------------------------------------");
        sala.broadcast("⚠️ " + actor.getId() + " quiere usar " + accion);

        if ("ASESINATO".equals(partida.getAccionPendiente()) && victima != null) {
            for (UnCliente c : sala.obtenerIntegrantes()) {
                if (c.equals(victima.getCliente())) {
                    c.salida().writeUTF("\n!!! TE QUIEREN ASESINAR !!!");
                    c.salida().writeUTF(" [1] Desafiar | [2] Permitir | [3] Bloquear (Condesa)");
                } else {
                    c.salida().writeUTF("Esperando decisión de la víctima...");
                }
            }
        }
        else if ("AYUDA".equals(partida.getAccionPendiente())) {
            for (UnCliente c : sala.obtenerIntegrantes()) {
                if (!c.equals(actor.getCliente())) {
                    c.salida().writeUTF("¿Respuesta? [1] Desafiar | [2] Permitir | [3] Bloquear (Duque)");
                } else {
                    c.salida().writeUTF("Esperando respuesta...");
                }
            }
        }
        else if ("EXTORSIÓN".equals(partida.getAccionPendiente()) && victima != null) {
            for (UnCliente c : sala.obtenerIntegrantes()) {
                if (c.equals(victima.getCliente())) {
                    c.salida().writeUTF("\n!!! TE QUIEREN EXTORSIONAR !!!");
                    c.salida().writeUTF(" [1] Desafiar | [2] Permitir | [3] Bloquear (Capitán) | [4] Bloquear (Embajador)");
                } else {
                    c.salida().writeUTF("Esperando decisión de la víctima...");
                }
            }
        }
        else {
            for (UnCliente c : sala.obtenerIntegrantes()) {
                if (!c.equals(actor.getCliente())) {
                    c.salida().writeUTF("¿Respuesta? [1] Desafiar | [2] Permitir");
                } else {
                    c.salida().writeUTF("Esperando respuesta...");
                }
            }
        }

        sala.broadcast("------------------------------------------------\n");
        iniciarTimerDesafio(partida, sala);
    }

    public void manejarInicioPartida(UnCliente cliente, Sala sala) throws IOException {
        if (sala != null && sala.getAdministrador().equals(cliente)) {
            if(sala.obtenerIntegrantes().size()<3 || sala.obtenerIntegrantes().size()>6){
                cliente.salida().writeUTF("al menos deben ser 3 jugadores, maximo 6");
                return;
            }
            Partida nuevaPartida = new Partida(sala.obtenerIntegrantes());
            sala.setPartida(nuevaPartida);
            sala.broadcast(">>> ¡PARTIDA INICIADA! <<<");

            for (UnCliente c : sala.obtenerIntegrantes()) {
                Jugador j = nuevaPartida.getJugador(c);
                if (j != null) mostrarEstadoJugador(j);
            }

            Jugador primer = nuevaPartida.obtenerJugadorTurno();
            sala.broadcast("Turno de: " + primer.getId());
            enviarMenuAcciones(primer.getCliente());
            iniciarTimerTurno(nuevaPartida, sala, primer);
        }
    }
}