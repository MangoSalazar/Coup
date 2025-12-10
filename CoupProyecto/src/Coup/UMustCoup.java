package Coup;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UMustCoup {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<Jugador> listaJugadores = new ArrayList<>();

        listaJugadores.add(new Jugador("Milton"));
        listaJugadores.add(new Jugador("Mango"));
        listaJugadores.add(new Jugador("Arturo"));

        EstadoDelJuego juego = new EstadoDelJuego(listaJugadores);

        while (true) {
            Jugador jugadorActual = juego.obtenerJugadorActual();
            System.out.println("\n--------------------------------");
            System.out.println("Turno de " + jugadorActual);

            Jugador objetivo = null;
            for(Jugador j : juego.obtenerJugadores()) {
                if(!j.equals(jugadorActual) && j.estaVivo()) {
                    objetivo = j;
                    break;
                }
            }

            System.out.println("Objetivo por defecto: " + (objetivo!=null ? objetivo.obtenerNombre() : "Nadie"));

            System.out.println("Acciones:");
            System.out.println("1. Ingreso (1 moneda)");
            System.out.println("2. Ayuda Extranjera (2 monedas)");
            System.out.println("3. Golpe (7 monedas)");
            System.out.println("4. Impuestos (3 monedas)");
            System.out.println("5. Asesinato (3 monedas)");
            System.out.println("6. Extorsion (Robar 2)");
            System.out.println("7. Cambio (Cartas)");
            System.out.print("Elige opcion: ");

            int opcion = sc.nextInt();

            Accion accion = new Accion(jugadorActual, objetivo, juego);

            int cartaAfectada = 0;

            switch (opcion) {
                case 1:
                    accion.ingreso();
                    break;
                case 2:
                    accion.ayudaExtrangera();
                    break;
                case 3:
                    if (objetivo != null) accion.golpe(cartaAfectada);
                    break;
                case 4:
                    accion.impuestos();
                    break;
                case 5:
                    if (objetivo != null) accion.asesinato(cartaAfectada);
                    break;
                case 6:
                    if (objetivo != null) accion.extorision();
                    break;
                case 7:
                    accion.cambio();
                    break;
                default:
                    System.out.println("Opcion no valida");
            }

            int vivos = 0;
            for(Jugador j : juego.obtenerJugadores()) if(j.estaVivo()) vivos++;
            if(vivos <= 1) {
                System.out.println("¡JUEGO TERMINADO!");
                break;
            }

            juego.siguienteTurno();
        }
    }
}