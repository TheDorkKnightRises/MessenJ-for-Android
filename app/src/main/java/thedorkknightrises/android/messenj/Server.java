package thedorkknightrises.android.messenj;

import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
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
    private TextView textArea;
    private TextView infoText;
    private TextInputEditText inputField;
    private FloatingActionButton sendButton;
    private ScrollView scrollView;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private ServerSocket serverSocket;
    private Socket socket;
    private int port;
    private String user;
    private int connections;
    private int number;
    private ClientHandler[] clientHandlers;
    boolean backFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle b = getIntent().getBundleExtra("extra");
        this.port = b.getInt("port");
        this.user = b.getString("user");
        this.number = b.getInt("number");
        connections = 0;

        inputField = (TextInputEditText) findViewById(R.id.inputField);
        textArea = (TextView) findViewById(R.id.chatText);
        infoText = (TextView) findViewById(R.id.info);
        scrollView= (ScrollView) findViewById(R.id.scrollView);

        sendButton = (FloatingActionButton) findViewById(R.id.fab);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = inputField.getText().toString().trim();
                if (!text.equals("")) {
                    send(user + ": " + text);
                    inputField.setText("");
                }
            }
        });
        inputField.requestFocus();
        allowTyping(false);
        clientHandlers = new ClientHandler[number];
        for (int i = 0; i < number; i++) clientHandlers[i] = null;
        try {
            serverSocket = new ServerSocket(port);
            infoText.setText("Connected as "+user+" on port "+port);
            showMessage("Waiting to connect...");
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    while (connections < number)
                    waitForConnection(connections);
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
                Looper.prepare();
                for (int i = 0; i < number; i++) {
                    try {
                        clientHandlers[i].outputStream.writeObject(text);
                        clientHandlers[i].outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Couldn\'t send your message to user #" + i);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                showMessage(text);
            }
        }).start();
    }

    void showMessage(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textArea.append(text + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
                inputField.requestFocus();
            }
        });
    }

    void waitForConnection(int n) {
        if (clientHandlers[n] == null) {
            try {
                clientHandlers[n] = new ClientHandler(this, serverSocket.accept(), n);
                send("New client connected");
                new Thread(clientHandlers[n]).start();
                connections++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    void disconnected(int number) {
        clientHandlers[number] = null;
        connections--;
        waitForConnection(number);
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
        for (int i = 0; i < number; i++) if (clientHandlers[i] != null) clientHandlers[i].close();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onBackPressed();
    }
}
