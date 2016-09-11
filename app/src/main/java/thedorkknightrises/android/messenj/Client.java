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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class Client extends AppCompatActivity {
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    Socket socket;
    String serverIP;
    int serverPort;
    String user;
    private TextView textArea;
    private TextView infoText;
    private TextInputEditText inputField;
    private ScrollView scrollView;
    private FloatingActionButton sendButton;
    boolean backFlag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle b = getIntent().getBundleExtra("extra");
        String host = b.getString("host");
        int port = b.getInt("port");
        String user = b.getString("user");
        if (!user.equals("")) {
            this.user = user;
        } else this.user = "Client";
        serverIP = host;
        serverPort = port;

        inputField = (TextInputEditText) findViewById(R.id.inputField);
        textArea = (TextView) findViewById(R.id.chatText);
        infoText = (TextView) findViewById(R.id.info);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        sendButton = (FloatingActionButton) findViewById(R.id.fab);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = inputField.getText().toString().trim();
                if (!text.equals("")) {
                    send(text);
                    inputField.setText("");
                }
            }
        });
        allowTyping(false);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                connect();
                setupStreams();
                whileConnected();
                close();
            }
        };
        new Thread(r).start();
        inputField.requestFocus();
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
                scrollView.fullScroll(View.FOCUS_DOWN);
                inputField.requestFocus();
            }
        });
    }

    void connect() {
        try {
            showMessage("Attempting connection to server...");
            socket = new Socket(InetAddress.getByName(serverIP), serverPort);
            showMessage("Connecting to " + socket.getInetAddress().getHostName()+" (waiting in queue)");
        } catch (ConnectException e) {
            e.printStackTrace();
            showMessage("Could not connect to server at that address");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void setupStreams() {
        if (socket != null) {
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.flush();
                inputStream = new ObjectInputStream(socket.getInputStream());
                showMessage("Connection established");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoText.setText("Connected as " + user + " to " + socket.getInetAddress().getHostName() + ":" + serverPort);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        super.onBackPressed();
    }
}
