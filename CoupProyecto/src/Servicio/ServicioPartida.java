package Servicio;

import Coup.Carta;
import Coup.Jugador;
import Dominio.Partida;
import Dominio.Sala;
import Servidor.UnCliente;
import java.io.IOException;
import java.util.TimerTask;

public class ServicioPartida {

    private UnCliente cliente;

    public ServicioPartida(UnCliente cliente) {
        this.cliente = cliente;
    }

    public void manejarAccionDeJuego(String comando, Sala sala) throws IOException {
        if (sala == null || !sala.estaEnPartida()) {
            cliente.salida().writeUTF("Error: No hay partida activa.");
            return;
        }

        Partida partida = sala.getPartida();

        // 1. Prioridad: Resolver revelación
        if (comando.startsWith("/revelar")) {
            manejarRevelacion(comando, sala, partida);
            return;
        }

        // 2. Prioridad: Intercambio Embajador
        if (comando.startsWith("/seleccionar")) {
            if (partida.getJugadorIntercambio() != null && partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                partida.cancelarTemporizador();
            }
            manejarSeleccionEmbajador(comando, sala, partida);
            return;
        }

        // Estados de pausa
        if (partida.getJugadorVictima() != null) {
            if (partida.getJugadorVictima().getCliente().equals(cliente)) {
                cliente.salida().writeUTF("¡Debes elegir carta a perder! Usa: /revelar [Carta]");
            } else {
                cliente.salida().writeUTF("Juego pausado. Esperando a que " + partida.getJugadorVictima().getId() + " pierda una carta.");
            }
            return;
        }

        if (partida.getJugadorIntercambio() != null) {
            if (partida.getJugadorIntercambio().getCliente().equals(cliente)) {
                cliente.salida().writeUTF("Estás cambiando cartas (20s). Usa: /seleccionar [Carta1] [Carta2]");
            } else {
                cliente.salida().writeUTF("Esperando a que " + partida.getJugadorIntercambio().getId() + " termine.");
            }
            return;
        }

        // 3. Manejo de Desafíos
        if (comando.startsWith("/desafiar") || comando.startsWith("/permitir")) {
            partida.cancelarTemporizador();
            if (comando.startsWith("/desafiar")) manejarDesafio(sala, partida);
            else manejarPermiso(sala, partida);
            return;
        }

        if (partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("¡Hay una acción esperando! Usa /desafiar o /permitir.");
            return;
        }

        // 4. Turno normal
        if (!partida.esTurnoDe(cliente)) {
            cliente.salida().writeUTF("¡No es tu turno!");
            return;
        }

        partida.cancelarTemporizador();

        Jugador jugadorActual = partida.getJugador(cliente);

        if (partida.debeDarGolpe(jugadorActual) && !comando.startsWith("/golpe")) {
            cliente.salida().writeUTF("¡Tienes 10+ monedas! Debes usar: /golpe [jugador]");
            iniciarTimerTurno(partida, sala, jugadorActual);
            return;
        }

        String[] partes = comando.split(" ");
        String accion = partes[0];

        switch (accion) {
            case "/ingresos":
                partida.accionIngresos(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " tomó INGRESOS (+1 moneda).");
                avanzarTurno(partida, sala);
                break;
            case "/ayuda":
                partida.accionAyudaExterior(jugadorActual);
                sala.broadcast(">> " + cliente.getId() + " pidió AYUDA EXTERIOR (+2 monedas).");
                avanzarTurno(partida, sala);
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
                        sala.broadcast("!!! " + victima.getId() + " fue eliminado por GOLPE.");
                        if(!victima.estaVivo()) sala.broadcast("☠ JUGADOR ELIMINADO: " + victima.getId());
                        avanzarTurno(partida, sala);
                    }
                } else {
                    iniciarTimerTurno(partida, sala, jugadorActual);
                }
                break;
            case "/impuestos":
                partida.setAccionPendiente("IMPUESTOS", jugadorActual, null, "DUQUE");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "IMPUESTOS", "DUQUE", null);
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
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "ASESINAR", "ASESINO", victima);
                } else {
                    iniciarTimerTurno(partida, sala, jugadorActual);
                }
                break;
            case "/extorsionar":
                if (procesarAtaque(partes, sala, partida, jugadorActual, "EXTORSIÓN")) {
                    Jugador victima = obtenerVictima(partes, sala, partida);
                    partida.setAccionPendiente("EXTORSIÓN", jugadorActual, victima, "CAPITAN");
                    anunciarAccionDesafiable(sala, partida, jugadorActual, "EXTORSIONAR", "CAPITAN", victima);
                } else {
                    iniciarTimerTurno(partida, sala, jugadorActual);
                }
                break;
            case "/cambio":
                partida.setAccionPendiente("CAMBIO", jugadorActual, null, "EMBAJADOR");
                anunciarAccionDesafiable(sala, partida, jugadorActual, "CAMBIO", "EMBAJADOR", null);
                break;
            default:
                cliente.salida().writeUTF("Acción no válida.");
                iniciarTimerTurno(partida, sala, jugadorActual);
        }
    }

    // --- TIMERS ---

    private void iniciarTimerTurno(Partida partida, Sala sala, Jugador jugador) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Si tiene 10 o más monedas, DEBE dar golpe, no puede tomar ingresos.
                    if (jugador.getMonedas() >= 10) {
                        Jugador victima = partida.obtenerVictimaAleatoria(jugador);

                        if (victima != null) {
                            sala.broadcast("\n⌛ TIEMPO AGOTADO (10+ monedas).");
                            sala.broadcast(">> ¡GOLPE DE ESTADO AUTOMÁTICO contra " + victima.getId() + "!");

                            int res = partida.iniciarGolpe(jugador, victima);
                            if (res == 1) {
                                sala.broadcast("!!! " + victima.getId() + " pierde una influencia.");
                                solicitarCartaAPerder(victima);
                                partida.setJugadorVictima(victima);
                                // NO avanzamos turno, esperamos a que la víctima revele carta
                            } else {
                                sala.broadcast("!!! " + victima.getId() + " ha sido ELIMINADO.");
                                avanzarTurno(partida, sala);
                            }
                        } else {
                            // Caso raro (solo queda 1 vivo? debería haber ganado ya)
                            avanzarTurno(partida, sala);
                        }

                    } else {
                        // Caso normal < 10 monedas
                        sala.broadcast("\n⌛ TIEMPO AGOTADO para " + jugador.getId() + ".");
                        sala.broadcast(">> Se aplican INGRESOS automáticos.");
                        partida.accionIngresos(jugador);
                        avanzarTurno(partida, sala);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 10000); // 10 Segundos turno
    }

    private void iniciarTimerDesafio(Partida partida, Sala sala) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    sala.broadcast("\n⌛ NADIE DESAFIÓ. La acción procede automáticamente.");
                    ejecutarAccionPendiente(sala, partida);
                    if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                        avanzarTurno(partida, sala);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 10000); // 10 Segundos desafío
    }

    private void iniciarTimerSeleccion(Partida partida, Sala sala, Jugador jugador) {
        partida.iniciarTemporizador(new TimerTask() {
            @Override
            public void run() {
                try {
                    sala.broadcast("\n⌛ TIEMPO DE SELECCIÓN AGOTADO (20s).");
                    sala.broadcast(">> " + jugador.getId() + " no eligió a tiempo. Cartas devueltas.");
                    partida.cancelarIntercambio();
                    avanzarTurno(partida, sala);
                } catch (IOException e) { e.printStackTrace(); }
            }
        }, 20000); // 20 Segundos Embajador
    }

    // --- FLUJO ---

    private void anunciarAccionDesafiable(Sala sala, Partida partida, Jugador actor, String accion, String carta, Jugador victima) throws IOException {
        sala.broadcast("\n------------------------------------------------");
        sala.broadcast("⚠️  " + actor.getId() + " quiere usar " + accion + ".");
        sala.broadcast("Dice ser: [" + carta + "]" + (victima != null ? " contra " + victima.getId() : ""));
        sala.broadcast("¿Le crees? Tienes 10 SEGUNDOS para: /desafiar o /permitir.");
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
            sala.broadcast(">> " + actor.getId() + " MOSTRÓ LA CARTA: " + cartaRequerida + ". ¡Es INOCENTE!");
            sala.broadcast("❌ " + desafiante.getId() + " falló el desafío.");
            partida.cambiarCartaPorGanarDesafio(actor, cartaRequerida);
            actor.getCliente().salida().writeUTF("Has robado una nueva carta.");
            ejecutarAccionPendiente(sala, partida);
            aplicarPenalizacion(sala, partida, desafiante);
        } else {
            sala.broadcast(">> " + actor.getId() + " NO TIENE " + cartaRequerida + ". ¡Es CULPABLE!");
            sala.broadcast("❌ La acción se cancela.");
            partida.limpiarAccionPendiente();
            aplicarPenalizacion(sala, partida, actor);
        }
    }

    private void manejarPermiso(Sala sala, Partida partida) throws IOException {
        if (!partida.hayAccionPendiente()) {
            cliente.salida().writeUTF("No hay nada que permitir.");
            return;
        }
        Jugador actor = partida.getActorPendiente();
        if (partida.getJugador(cliente).equals(actor)) {
            cliente.salida().writeUTF("Espera a los demás.");
            iniciarTimerDesafio(partida, sala);
            return;
        }
        sala.broadcast(cliente.getId() + " ha permitido la jugada.");
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
                if (!victima.estaVivo()) {
                    sala.broadcast("El objetivo ya estaba muerto.");
                } else {
                    if (res == 1) {
                        sala.broadcast("!!! " + victima.getId() + " ha sido ASESINADO.");
                        solicitarCartaAPerder(victima);
                    } else {
                        sala.broadcast("!!! " + victima.getId() + " eliminado por Asesinato.");
                    }
                }
                break;
            case "EXTORSIÓN":
                partida.accionExtorsion(actor, victima);
                sala.broadcast(">> " + actor.getId() + " extorsionó a " + victima.getId());
                break;
            case "CAMBIO":
                partida.iniciarEmbajador(actor);
                sala.broadcast(">> " + actor.getId() + " inició CAMBIO.");
                mostrarOpcionesEmbajador(actor);
                iniciarTimerSeleccion(partida, sala, actor);
                break;
        }
    }

    private void aplicarPenalizacion(Sala sala, Partida partida, Jugador perdedor) throws IOException {
        if (perdedor.getInfluenciaActiva() <= 1) {
            perdedor.perderInfluencia();
            sala.broadcast("☠ " + perdedor.getId() + " perdió su última carta y ha sido ELIMINADO.");
            if (partida.getJugadorIntercambio() == null && partida.getJugadorVictima() == null) {
                avanzarTurno(partida, sala);
            }
        } else {
            solicitarCartaAPerder(perdedor);
            partida.setJugadorVictima(perdedor);
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

        StringBuilder cartas = new StringBuilder();
        for(Carta c : siguiente.getMano()) if(!c.estaRevelada()) cartas.append("[").append(c.verNombre()).append("] ");
        siguiente.getCliente().salida().writeUTF("Tus cartas: " + cartas.toString());
        enviarMenuAcciones(siguiente.getCliente());

        iniciarTimerTurno(partida, sala, siguiente);
    }

    private void enviarMenuAcciones(UnCliente cliente) throws IOException {
        String menu = "\n" +
                "--- ACCIONES (10s) ---\n" +
                "1. /ingresos       -> +1 moneda\n" +
                "2. /ayuda          -> +2 monedas\n" +
                "3. /impuestos      -> +3 monedas (Duque)\n" +
                "4. /asesinar [jug] -> Costo 3 (Asesino)\n" +
                "5. /extorsionar [jug] -> Roba 2 (Capitán)\n" +
                "6. /cambio         -> Cartas (Embajador)\n" +
                "7. /golpe [jug]    -> Costo 7\n" +
                "----------------\n";
        cliente.salida().writeUTF(menu);
    }

    private void mostrarOpcionesEmbajador(Jugador j) throws IOException {
        String opciones = "";
        for(Carta c : j.getMano()) if(!c.estaRevelada()) opciones += "[" + c.verNombre() + "] ";
        j.getCliente().salida().writeUTF("Cartas: " + opciones + "\nUsa: /seleccionar [Carta1] " + (j.getInfluenciaActiva() > 2 ? "[Carta2]" : ""));
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
            cliente.salida().writeUTF("Error en selección.");
        }
    }

    private void manejarRevelacion(String comando, Sala sala, Partida partida) throws IOException {
        Jugador victima = partida.getJugadorVictima();
        if (victima == null || !victima.getCliente().equals(cliente)) return;
        String[] partes = comando.split(" ");
        if (partes.length < 2) return;
        if (partida.concretarPerdida(victima, partes[1])) {
            sala.broadcast("☠ " + victima.getId() + " reveló: " + partes[1]);
            if (!victima.estaVivo()) sala.broadcast("☠ ELIMINADO: " + victima.getId());
            if (partida.getJugadorIntercambio() == null) avanzarTurno(partida, sala);
        } else {
            cliente.salida().writeUTF("Carta no válida.");
        }
    }

    private void solicitarCartaAPerder(Jugador victima) throws IOException {
        String cartas = "";
        for (Carta c : victima.getMano()) if (!c.estaRevelada()) cartas += "[" + c.verNombre() + "] ";
        victima.getCliente().salida().writeUTF("PERDISTE UN DESAFÍO/VIDA. Elige carta a revelar:\n" + cartas + "\nUsa: /revelar [Carta]");
    }

    private boolean procesarAtaque(String[] partes, Sala sala, Partida partida, Jugador atacante, String accion) throws IOException {
        if (partes.length < 2) {
            cliente.salida().writeUTF("Falta objetivo.");
            return false;
        }
        Jugador victima = obtenerVictima(partes, sala, partida);
        if (victima == null || !victima.estaVivo()) {
            cliente.salida().writeUTF("Jugador no válido.");
            return false;
        }
        if (victima.equals(atacante)) {
            cliente.salida().writeUTF("No puedes atacarte a ti mismo.");
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
            StringBuilder cartas = new StringBuilder();
            for(Carta c : primer.getMano()) if(!c.estaRevelada()) cartas.append("[").append(c.verNombre()).append("] ");
            primer.getCliente().salida().writeUTF("Tus cartas: " + cartas.toString());
            enviarMenuAcciones(primer.getCliente());

            iniciarTimerTurno(nuevaPartida, sala, primer);
        }
    }
}