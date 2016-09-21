package thedorkknightrises.messenj;

import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 * Created by Samriddha Basu on 9/10/2016.
 */
public class Server extends AppCompatActivity {
    private TextView textArea;
    private TextView infoText;
    private TextInputEditText inputField;
    private FloatingActionButton sendButton;
    private ScrollView scrollView;
    private ServerSocket serverSocket;
    private int port;
    private String user;
    private int connections;
    private int number;
    private ClientHandler[] clientHandlers;
    boolean backFlag = false;
    public String[] users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle b = getIntent().getBundleExtra("extra");
        this.port = b.getInt("port");
        this.user = b.getString("user");
        this.number = b.getInt("number");
        connections = 0;

        users = new String[number + 1];
        users[0] = user;

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
                    send(new Message(Message.TYPE_TEXT, text, user));
                    inputField.setText("");
                    inputField.requestFocus();
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
            showMessage(new Message("Waiting to connect..."));
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
            showMessage(new Message("This port is already in use. Try hosting on a different port"));
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

    void send(final Message message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                allowTyping(false);
                for (int i = 0; i < number; i++) {
                    try {
                        clientHandlers[i].outputStream.writeObject(message);
                        clientHandlers[i].outputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage(new Message("Couldn\'t send your message to user #" + i));
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                    }
                }
                showMessage(message);
                allowTyping(true);
            }
        }).start();
    }

    void showMessage(final Message message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (message.getType()) {
                    case Message.TYPE_CONNECT:
                        textArea.append(message.getText() + "\n");
                        users[message.getNumber() + 1] = message.getSender();
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    case Message.TYPE_DISCONNECT:
                        users[message.getNumber() + 1] = null;
                    case Message.TYPE_ANNOUNCE:
                        textArea.append(message.getText() + "\n");
                        scrollView.fullScroll(View.FOCUS_DOWN);
                        break;
                    case Message.TYPE_TEXT:
                        textArea.append(message.getSender() + ": " + message.getText() + "\n");
                        scrollView.fullScroll(View.FOCUS_DOWN);
                        break;
                    case Message.TYPE_FILE:
                        try {
                            new File(System.getProperty("user.home")
                                    + File.separator
                                    + "MessenJ")
                                    .mkdirs();
                            File newFile = new File(System.getProperty("user.home")
                                    + File.separator
                                    + "MessenJ"
                                    + File.separator
                                    + message.getText());
                            showMessage(new Message(message.getSender() + " is sending file: " + message.getText()));
                            FileOutputStream writer = new FileOutputStream(newFile);
                            writer.write(message.getData());
                            writer.close();
                            showMessage(new Message("File transfer complete"));
                            showMessage(new Message("Saved to: " + newFile.getPath()));
                        } catch (IOException e) {
                            showMessage(new Message("Error transferring file"));
                        }
                        break;
                }
            }});
    }

    void waitForConnection(int n) {
        if (clientHandlers[n] == null) {
            try {
                clientHandlers[n] = new ClientHandler(this, serverSocket.accept(), n);
                showMessage(new Message("Incoming connection..."));
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
        if (users[number + 1] != null)
            send(new Message(Message.TYPE_DISCONNECT, number, users[number + 1] + " disconnected", users[number + 1]));
        users[number + 1] = null;
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
