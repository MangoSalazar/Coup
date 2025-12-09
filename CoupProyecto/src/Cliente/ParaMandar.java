package Cliente;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
public class ParaMandar implements Runnable{
    String[] mensajitos = {"hola","12345","ola","prueba mandada"};
    int n = 0;
    final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    final DataOutputStream salida ;
    public ParaMandar(Socket s) throws IOException {
        this.salida = new DataOutputStream(s.getOutputStream());
    }

    @Override
    public void run() {
        mandarMensaje();
    }
        public void mandarMensaje() {
        int nMensaje = 0;
        while (true) {
            String mensaje = "";
            try {
                nMensaje = enviarTest(nMensaje, mensaje);
                /*
                mensaje = teclado.readLine();
                salida.writeUTF(mensaje);
                */
            } catch (IOException ex) {
                
            }

        }
    }
    public int enviarTest(int nMensaje, String mensaje) throws IOException {
        while (nMensaje < mensajitos.length) {
            mensaje = mensajitos[nMensaje];
            salida.writeUTF(mensaje);
            nMensaje++;
        }
        return nMensaje;
    }
}
 