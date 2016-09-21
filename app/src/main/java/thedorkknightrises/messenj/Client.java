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

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

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
    String users[];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Bundle b = getIntent().getBundleExtra("extra");
        String host = b.getString("host");
        int port = b.getInt("port");
        final String user = b.getString("user");
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
                    send(new Message(Message.TYPE_TEXT, text, user));
                    inputField.setText("");
                    inputField.requestFocus();
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

    void send(final Message message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                allowTyping(false);
                try {
                    outputStream.writeObject(message);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage(new Message("Couldn\'t send your message"));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
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
                            e.printStackTrace();
                        }
                        break;
                }
            }
        });
    }

    void connect() {
        try {
            showMessage(new Message("Attempting connection to server..."));
            socket = new Socket(InetAddress.getByName(serverIP), serverPort);
            showMessage(new Message("Connecting to " + socket.getInetAddress().getHostName() + " (waiting in queue)"));
        } catch (IOException e) {
            showMessage(new Message("Could not connect to server at that address"));
        }
    }

    void setupStreams() {
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            outputStream.writeObject(user);
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            showMessage(new Message("Connection established"));
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  infoText.setText("Connected as " + user + " to " + socket.getInetAddress().getHostName() + ":" + serverPort);
                              }
                          });
            users = (String[]) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            showMessage(new Message("Error fetching list of users"));
            e.printStackTrace();
        } catch (NullPointerException e) {
            showMessage(new Message("Something went wrong"));
        }
    }

    void whileConnected() {
        Message message = null;
        allowTyping(true);
        do {
            try {
                message = (Message) inputStream.readObject();
                showMessage(message);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                showMessage(new Message("Something went wrong, cannot display message"));
            } catch (EOFException e) {
            } catch (SocketException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        } while (true);
    }

    void close() {
        showMessage(new Message("Closing all connections..."));
        try {
            inputStream.close();
            outputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            showMessage(new Message("All connections closed."));
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
