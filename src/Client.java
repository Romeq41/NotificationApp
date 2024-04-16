import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static java.lang.System.exit;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Thread userInputThread = new Thread(new InputHandler(socket));
            userInputThread.start();

            String serverResponse;
            while ((serverResponse = in.readLine()) != null) {
                System.out.println("Server: " + serverResponse);
            }


            socket.close();
        } catch (IOException e) {
            System.out.println("Serwer jest wyłączony");
            exit(0);
        }
    }

    static class InputHandler implements Runnable {
        private final Socket socket;

        public InputHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("Podaj treść notyfikacji oraz czas wysłania w formacie godzina:minuta:sekunda po przecinku lub pustą linie by zakończyć");

                String userInputLine;
                while ((userInputLine = userInput.readLine()) != null) {
                    if (userInputLine.isEmpty()) {
                        System.out.println("Zamykanie portów...");
                        socket.close();
                        break;
                    }

                    out.println(userInputLine);
                }
            } catch (IOException e) {
                System.out.println("Input error: " + e.getMessage());
            }
        }
    }
}