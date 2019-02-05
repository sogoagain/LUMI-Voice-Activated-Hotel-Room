package io.github.sogoagain.lumi;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import java.io.IOException;
import java.net.*;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "LUMI_" + SignupActivity.class.getSimpleName();

    private static final String serverIP = "0.0.0.0";
    private static final int serverPORT = 4001;

    private String reservationNumber = "";

    private ProgressDialog progressDialog;

    @InjectView(R.id.booking_date) EditText _dateText;
    @InjectView(R.id.booking_name) EditText _nameText;
    @InjectView(R.id.booking_phone) EditText _phoneText;
    @InjectView(R.id.btn_signup) Button _signupButton;
    @InjectView(R.id.link_login) TextView _loginLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.inject(this);

        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });
    }


    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        progressDialog = new ProgressDialog(SignupActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Making a reservation...");
        progressDialog.show();

        String name = _nameText.getText().toString();
        String date = _dateText.getText().toString();
        String phone = _phoneText.getText().toString();

        // TODO: Implement your own signup logic here.
        new SignupTask().execute(name, date, phone);
    }


    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        Toast.makeText(SignupActivity.this, "예약번호: " + reservationNumber, Toast.LENGTH_LONG).show();
        finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Reservation failed", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String date = _dateText.getText().toString();
        String phone = _phoneText.getText().toString();

        if (name.isEmpty()) {
            _nameText.setError("이름을 입력해주세요.");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (date.isEmpty() || date.length() != 8) {
            _dateText.setError("날짜 형식 YYYYMMDD");
            valid = false;
        } else {
            _dateText.setError(null);
        }

        if (phone.isEmpty() || phone.length() != 11) {
            _phoneText.setError("-없이 입력해주세요.");
            valid = false;
        } else {
            _phoneText.setError(null);
        }

        return valid;
    }

    public class SignupTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO: attempt authentication against a network service.

            String name = params[0];
            String date = params[1];
            String phone = params[2];

            Log.d(TAG, name);
            Log.d(TAG, date);
            Log.d(TAG, phone);

            String command = phone + ":" + date;
            Log.d(TAG + "_NETWORK", "전송문자열:" + command);

            try {

                DatagramSocket socket = new DatagramSocket();
                InetAddress inetAddress = InetAddress.getByName(serverIP);

                byte[] buf = (command).getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, serverPORT);

                socket.send(packet);

                socket.receive(packet);
                socket.close();
                String msg = new String(packet.getData());

                if(msg.indexOf('@') == -1) {
                    Log.d(TAG + "_NETWORK", "@ 없음");
                    return false;
                }

                Log.d(TAG+"_NETWORK", msg.indexOf('@') + ", receive:" + msg);

                msg = msg.substring(0, msg.indexOf('@'));
                Log.d(TAG + "_NETWORK", "parsing_receive:" + msg);

                if(msg.equals("N") || msg.equals("n")) {
                    return  false;
                } else {
                    reservationNumber = msg;
                    return true;
                }
            } catch (SocketException e) {
                e.printStackTrace();
                return false;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            progressDialog.dismiss();

            if (success) {
                onSignupSuccess();
            } else {
                onSignupFailed();
            }
        }

        @Override
        protected void onCancelled() {
            progressDialog.dismiss();
        }
    }
}
