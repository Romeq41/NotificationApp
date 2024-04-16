import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Server {
    private static final int PORT = 12345;
    private static final ConcurrentLinkedQueue<Notification> notificationQueue = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serwer uruchomiony. Oczekiwanie na połączenia...");

            Thread autoRefreshThread = new Thread(new AutoRefresh());
            autoRefreshThread.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nowe połączenie: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException a) {
            System.out.println("Zły numer portu");
        }
    }

    public record Notification(String content, long timestamp, Socket socket) {
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {

                    try {
                        long expectedDeliveryTime;
                        String[] clientInput;
                        Notification note;

                        clientInput = inputLine.split(",");
                        String timeStr = clientInput[1];

                        final Pattern pattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$");
                        Matcher matcher = pattern.matcher(timeStr);

                        if (!matcher.matches()) {
                            throw new InvalidTimeFormatException();
                        }

                        Date today = new Date();
                        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
                        String todayStr = dateFormatter.format(today);

                        DateFormat combinedFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date combinedDate = combinedFormat.parse(todayStr + " " + timeStr);
                        expectedDeliveryTime = combinedDate.getTime();

                        if (expectedDeliveryTime < System.currentTimeMillis()) {
                            throw new ThisTimeHasAlreadyPassedException();
                        }

                        note = new Notification(clientInput[0], expectedDeliveryTime, clientSocket);
                        System.out.println("Wiadomość od klienta: " + clientSocket + " o treści: " + clientInput[0] + ". Do wysłania o: " + combinedFormat.format(combinedDate));

                        notificationQueue.offer(note);

                        out.println("Notyfikacja została odebrana przez serwer.");
                    } catch (ArrayIndexOutOfBoundsException | ParseException ex) {
                        out.println("Niepoprawny format. Wymagany wiadomość,HH:MM:SS");
                    } catch (InvalidTimeFormatException | ThisTimeHasAlreadyPassedException e) {
                        System.out.println(e.getMessage());
                        out.println(e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Błąd połączenia. Usuwanie zbędnych powiadomień");
                for (Notification note : notificationQueue)
                    if (note.socket() == clientSocket)
                        notificationQueue.remove(note);
            }
        }
    }

    static class AutoRefresh implements Runnable {
        private static void sendNotificationToClient() {
            long currentTime = System.currentTimeMillis();

            for (Notification clientNotification : notificationQueue) {
                long sendTime = clientNotification.timestamp();
                if (sendTime <= currentTime) {
                    try {
                        PrintWriter out = new PrintWriter(clientNotification.socket().getOutputStream(), true);
                        out.println("Odebrano zaplanowaną notyfikację: " + clientNotification.content());
                        System.out.println("Sent scheduled notification: " + clientNotification.content());
                    } catch (IOException e) {
                        System.out.println("Błąd podczas wysyłania notyfikacji.");
                    }

                    notificationQueue.remove(clientNotification);
                }
            }
        }

        public void run() {
            while (true) {
                sendNotificationToClient();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("run Blad");
                }
            }
        }
    }
}