package thedorkknightrises.android.messenj;

import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Samriddha Basu on 9/10/2016.
 */
public class Server extends AppCompatActivity {

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private ServerSocket serverSocket;
    private Socket socket;
    private int port;
    private String user;
    private TextView textArea;
    private TextView infoText;
    private EditText inputField;
    private FloatingActionButton sendButton;
    boolean backFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle b = getIntent().getBundleExtra("extra");
        int port = b.getInt("port");
        String user = b.getString("user");
        this.port = port;
        if (!user.equals("")) {
            this.user = user;
        } else this.user = "SERVER";

        inputField = (EditText) findViewById(R.id.inputField);
        textArea = (TextView) findViewById(R.id.chatText);
        infoText = (TextView) findViewById(R.id.info);

        sendButton = (FloatingActionButton) findViewById(R.id.fab);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = inputField.getText().toString();
                if (!text.equals("")) {
                    send(text);
                    inputField.setText("");
                }
            }
        });
        inputField.requestFocus();
        allowTyping(false);
        try {
            serverSocket = new ServerSocket(port, 5);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    waitForConnection();
                    setupStreams();
                    whileConnected();
                    close();
                }
            };
            new Thread(r).start();

        } catch (BindException e) {
            e.printStackTrace();
            showMessage("This port is already in use. Try hosting on a different port");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void allowTyping(final boolean allowed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inputField.setEnabled(allowed);
                sendButton.setEnabled(allowed);       }
        });
    }

    void send(final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (text.equals("END")) {
                        outputStream.writeObject(text);
                        outputStream.flush();
                    } else {
                        outputStream.writeObject(user + ": " + text);
                        outputStream.flush();
                        showMessage(user + ": " + text);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("Couldn\'t send your message");
                }
            }
        }).start();
    }

    void showMessage(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textArea.append(text + "\n");
            }
        });
    }

    void waitForConnection() {
        try {
            showMessage("Waiting to connect...");
            socket = serverSocket.accept();
            showMessage("Connecting to " + socket.getInetAddress().getHostName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setupStreams() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            showMessage("Connection established");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    infoText.setText("Connected as " + user + " to " + socket.getInetAddress().getHostName() + ":" + port);                }
            });
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    void whileConnected() {
        String message = "";
        allowTyping(true);
        do {
            try {
                message = (String) inputStream.readObject();
                showMessage(message);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                showMessage("Something went wrong, cannot display message");
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                break;
            }
        } while (!message.equals("END"));
    }

    void close() {
        showMessage("Closing all connections...");
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            showMessage("All connections closed.");
            allowTyping(false);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    infoText.setText("Not connected");
                }
            });
        }
    }
    @Override
    public void onBackPressed() {
        if (!backFlag) {
            Toast.makeText(getApplicationContext(), "Press back again to disconnect",
                    Toast.LENGTH_SHORT).show();
            backFlag = true;

            // Thread to change backPressedFlag to false after 3000ms
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        backFlag = false;
                    }
                }
            }).start();
            return;
        }
        close();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onBackPressed();
    }
}
