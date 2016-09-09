package thedorkknightrises.android.messenj;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    private EditText hostView;
    private EditText portView;
    private EditText username;
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
        username = (EditText) findViewById(R.id.username);
        hostView = (EditText) findViewById(R.id.host);
        launch = (TextView) findViewById(R.id.launchMode);

        portView = (EditText) findViewById(R.id.port);
        portView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

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

