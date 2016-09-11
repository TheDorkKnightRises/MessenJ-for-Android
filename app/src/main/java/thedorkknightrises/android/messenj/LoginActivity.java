package thedorkknightrises.android.messenj;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    private TextInputEditText hostView;
    private TextInputEditText portView;
    private TextInputEditText username;
    private TextInputEditText numberView;
    private RadioButton serverRadioButton;
    private RadioButton clientRadioButton;
    private TextView launch;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = LoginActivity.this;
        // Set up the login form.
        username = (TextInputEditText) findViewById(R.id.username);
        hostView = (TextInputEditText) findViewById(R.id.host);
        numberView = (TextInputEditText) findViewById(R.id.number);
        launch = (TextView) findViewById(R.id.launchMode);

        portView = (TextInputEditText) findViewById(R.id.port);

        serverRadioButton = (RadioButton) findViewById(R.id.serverRadioButton);
        clientRadioButton = (RadioButton) findViewById(R.id.clientRadioButton);
        serverRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                clientRadioButton.setSelected(!serverRadioButton.isChecked());
                hostView.setEnabled(!serverRadioButton.isChecked());
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        hostView.setError(null);
        portView.setError(null);
        launch.setError(null);

        String hostname = hostView.getText().toString().trim();
        String portStr = portView.getText().toString().trim();
        String user = username.getText().toString().trim();
        int portNo = 8080;
        int number = 1;

        boolean cancel = false;
        View focusView = null;

        if (hostView.isEnabled() && TextUtils.isEmpty(hostname)) {
            hostView.setError(getString(R.string.error_field_required));
            focusView =  hostView;
            cancel = true;
        }

        if (TextUtils.isEmpty(portStr)) {
            portView.setError(getString(R.string.error_field_required));
            focusView =  portView;
            cancel = true;
        } else {
            portNo = Integer.parseInt(portStr);
        }

        if (TextUtils.isEmpty(user)) {
            username.setError(getString(R.string.error_field_required));
            focusView =  username;
            cancel = true;
        }

        if (!numberView.getText().toString().trim().equals("")) {
            number = Integer.parseInt(numberView.getText().toString().trim());
            if (number < 1 || number > 10) {
                numberView.setError("Must be between between 1 to 10");
                focusView = numberView;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            final Bundle b = new Bundle();
            b.putString("host", hostname);
            b.putInt("port", portNo);
            b.putString("user", user);
            b.putInt("number", number);
            if (clientRadioButton.isChecked()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Intent i = new Intent(context, Client.class);
                        i.putExtra("extra", b);
                        startActivity(i);
                    }
                }).start();
            } else if (serverRadioButton.isChecked()){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        Intent i = new Intent(context, Server.class);
                        i.putExtra("extra", b);
                        startActivity(i);
                    }
                }).start();
            } else launch.setError(getString(R.string.error_field_required));
        }
    }


}

