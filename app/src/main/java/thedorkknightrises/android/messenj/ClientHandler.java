package thedorkknightrises.android.messenj;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Samriddha Basu on 9/12/2016.
 */
public class ClientHandler implements Runnable {
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    private Server server;
    private Socket socket;
    private int number;

    public ClientHandler(Server server, Socket socket, int number) {
        this.server = server;
        this.socket = socket;
        this.number = number;
    }

    @Override
    public void run() {
        setup();
        whileConnected();
        close();
    }

    void setup() {
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void whileConnected() {
        String message = "";
        server.allowTyping(true);
        do {
            try {
                message = (String) inputStream.readObject();
                server.send(message);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                server.showMessage("Something went wrong, cannot display message");
            } catch (SocketException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        } while (!message.equals("END"));
    }

    void close() {
        server.showMessage("Closing connection with " + socket.getInetAddress().getHostName());
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.showMessage(socket.getInetAddress().getHostName() + " disconnected");
        }
        server.disconnected(number);
    }
}