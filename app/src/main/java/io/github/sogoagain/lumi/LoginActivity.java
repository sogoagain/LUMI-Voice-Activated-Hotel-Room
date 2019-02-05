package io.github.sogoagain.lumi;

import android.Manifest;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final String TAG = "LUMI_" + LoginActivity.class.getSimpleName();
    private static final String serverIP = "127.0.0.1";
    private static final int serverPORT = 4002;

    private static final int REQUEST_PERMISSIONS = 1;
    private boolean isPermitted = false;

    /**
     * 테스트용 더미 데이터
     * TODO: 테스트용 더미 데이터로 실제 빌드시 삭제
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "01012345678:2018020278:210", "01056781234:2018020234:304", "01087654321:2018020221:704"
    };
    private String roomNumber = "";
    private String roomIp = "";

    private UserLoginTask mAuthTask = null;
    private SharedPreferences sharedPreferences;

    private AutoCompleteTextView mPhoneView;
    private EditText mBookingView;
    private ProgressDialog progressDialog;
    private TextView signupLinkView;

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra(getString(R.string.intent_permission), isPermitted);
        intent.putExtra(getString(R.string.intent_room_number), roomNumber);
        startActivity(intent);

        LoginActivity.this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        populateAutoComplete();

        mPhoneView = findViewById(R.id.phone);
        mBookingView = findViewById(R.id.booking);
        mBookingView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mCheckInButton = findViewById(R.id.check_in_button);
        mCheckInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        signupLinkView = findViewById(R.id.link_signup);

        signupLinkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        progressDialog = new ProgressDialog(LoginActivity.this, R.style.AppTheme_Dark_Dialog);

        sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // keys는 sharedPreferences에 저장된 모든 데이터들을 Map의 형태로 갖고있다.
        Map<String,?> savedUserInfo = sharedPreferences.getAll();

        if(!savedUserInfo.isEmpty()) {
            startMainActivity();
        }

        Log.d(TAG +"_PERMISSION", "권한획득:" + isPermitted);
    }

    private void populateAutoComplete() {
        if (!(requestRuntimePermission())) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean requestRuntimePermission() {
        if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    Manifest.permission.RECORD_AUDIO)) {
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(LoginActivity.this,
                        new String[]{Manifest.permission.READ_CONTACTS,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSIONS);
            }
        } else {
            isPermitted = true;
            return true;
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (hasAllPermissionsGranted(grantResults)) {
                    isPermitted = true;
                    populateAutoComplete();
                } else {
                    isPermitted = false;
                }
            }
        }
    }

    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPhoneView.setError(null);
        mBookingView.setError(null);

        // Store values at the time of the login attempt.
        String phone = mPhoneView.getText().toString();
        String booking = mBookingView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(booking) && !isBookingValid(booking)) {
            mBookingView.setError(getString(R.string.error_invalid_booking));
            focusView = mBookingView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(phone)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            cancel = true;
        } else if (!isPhoneValid(phone)) {
            mPhoneView.setError(getString(R.string.error_invalid_phone));
            focusView = mPhoneView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            // showProgress(true);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Cheking In...");
            progressDialog.show();

            mAuthTask = new UserLoginTask(phone, booking);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPhoneValid(String phone) {
        //TODO: Replace this with your own logic
        return phone.length() == 11;
    }

    private boolean isBookingValid(String booking) {
        //TODO: Replace this with your own logic
        return booking.length() == 10;
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> phones = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            phones.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addPhonesToAutoComplete(phones);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addPhonesToAutoComplete(List<String> phoneNumberCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, phoneNumberCollection);

        mPhoneView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mPhone;
        private final String mBooking;

        UserLoginTask(String phone, String booking) {
            mPhone = phone;
            mBooking = booking;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress inetAddress = InetAddress.getByName(serverIP);

                String command = mPhone + ":" + mBooking;
                Log.d(TAG + "_NETWORK", "전송문자열: " + command);
                byte[] buf = (command).getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, serverPORT);

                socket.send(packet);

                //데이터 수신
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
                    return false;
                } else {
                    // 방번호:ip
                    StringTokenizer stringTokenizer = new StringTokenizer(msg, ":");
                    if(stringTokenizer.countTokens() == 2) {
                        roomNumber = stringTokenizer.nextToken();
                        roomIp = stringTokenizer.nextToken();
                    }
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
            mAuthTask = null;
            // showProgress(false);
            progressDialog.dismiss();

            if (success) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("ROOM",  roomNumber);
                editor.putString("IP", roomIp);
                editor.putString("PHONE", mPhone);
                editor.commit();

                startMainActivity();
            } else {
                mBookingView.setError(getString(R.string.error_incorrect_phone));
                mBookingView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            // showProgress(false);
            progressDialog.dismiss();
        }
    }
}

