package io.github.sogoagain.lumi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.speech.clientapi.SpeechRecognitionResult;
import com.romainpiel.shimmer.Shimmer;
import com.romainpiel.shimmer.ShimmerTextView;
import com.skyfishjy.library.RippleBackground;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.sogoagain.lumi.utils.AudioWriterPCM;

public class MainActivity extends Activity {

    private static final String TAG = "LUMI_" + MainActivity.class.getSimpleName();
    private static final String CLIENT_ID = "NAVER_SPEECH_API_KEY";

    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "OFF";

    /**
     * ROOM RASPBERRY
     */
    private static final String serverIP = "127.0.0.1";
    private static final int serverPort = 4002;
    private static final int roomServerPORT = 4000;
    private String roomServerIP = "";

    /**
     * STT
     */
    private NaverRecognizer naverRecognizer;
    private String mResult;
    private AudioWriterPCM writer;
    private NaturalLanguageProcessor naturalLanguageProcessor;

    /**
     * TTS
     */
    private MediaPlayer mediaPlayer = null;

    /**
     * UI
     */
    private TextView txtRoom;
    private FloatingActionButton btnSpeakNow;
    private RippleBackground rippleBackground;
    private Button btnCheckOut;
    private ShimmerTextView txtShimmer;
    private Shimmer shimmer;

    private String roomNumber;
    private String phone;
    private boolean isPermitted = false;
    private SharedPreferences sharedPreferences;

    private void startLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);

        MainActivity.this.finish();
    }

    private void changeUINowMicOn() {
        btnSpeakNow.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_mic_off));
        rippleBackground.startRippleAnimation();
    }

    private void changeUINowMicOff() {
        btnSpeakNow.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_mic_on));
        rippleBackground.stopRippleAnimation();
        btnSpeakNow.setEnabled(true);
    }

    private void readyMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
        }
    }

    private void releaseMediaPlayer(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
    }

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtShimmer.setText(R.string.lumi_call);

                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtShimmer.setText(mResult);
                Log.d(TAG + "_STT", "partial result:" + mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
                SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
                List<String> results = speechRecognitionResult.getResults();
                StringBuilder strBuf = new StringBuilder();
                strBuf.append(results.get(0));

                mResult = strBuf.toString();
                Log.d(TAG + "_STT", "final result: " + mResult);

                UserCommand userCommand = naturalLanguageProcessor.extractIntent(mResult);

                readyMediaPlayer(mediaPlayer);
                if (userCommand == null) {
                    txtShimmer.setText(R.string.lumi_error);
                    mediaPlayer = MediaPlayer.create(MainActivity.this,
                            new Random().nextBoolean() ? R.raw.lumi_error01 : R.raw.lumi_error02);
                } else {
                    Log.d(TAG + "_STT", "NLP:" + userCommand.getCommand());
                    txtShimmer.setText(R.string.lumi_success);
                    mediaPlayer = MediaPlayer.create(MainActivity.this,
                            new Random().nextBoolean() ? R.raw.lumi_success01 : R.raw.lumi_success02);
                    new RequestTask().execute(userCommand.getCommand());
                }
                mediaPlayer.start();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        readyMediaPlayer(mediaPlayer);
                    }
                });
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                Log.d(TAG + "_STT", "error:" + mResult);

                changeUINowMicOff();
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                changeUINowMicOff();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        naverRecognizer = new NaverRecognizer(this, new RecognitionHandler(this), CLIENT_ID);
        naturalLanguageProcessor = new NaturalLanguageProcessor();

        btnSpeakNow = findViewById(R.id.btn_speak_now);
        rippleBackground = findViewById(R.id.ripple_background);
        txtShimmer = findViewById(R.id.txt_shimmer);
        txtShimmer.setText(R.string.lumi_init);
        btnCheckOut = findViewById(R.id.btn_checkOut);
        txtRoom = findViewById(R.id.txt_room);

        btnSpeakNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    txtShimmer.setText(R.string.lumi_connecting);
                    changeUINowMicOn();

                    readyMediaPlayer(mediaPlayer);
                    mediaPlayer = MediaPlayer.create(MainActivity.this,
                            new Random().nextBoolean() ? R.raw.lumi_call01 : R.raw.lumi_call02);
                    mediaPlayer.start();

                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            readyMediaPlayer(mediaPlayer);
                            naverRecognizer.recognize();
                        }
                    });
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btnSpeakNow.setEnabled(false);
                    txtShimmer.setText(R.string.lumi_init);
                    changeUINowMicOff();

                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });

        btnCheckOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CheckOutTask().execute("O" + phone);
                new RequestTask().execute("ACT1:N ACT2:N ACT3:N");

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.commit();

                Toast.makeText(MainActivity.this, getString(R.string.good_bye), Toast.LENGTH_SHORT).show();
                startLoginActivity();
            }
        });

        Intent intent = getIntent();
        isPermitted = intent.getBooleanExtra(getString(R.string.intent_permission), false);

        sharedPreferences = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        Map<String, ?> savedUserInfo = sharedPreferences.getAll();
        roomNumber = (String) savedUserInfo.get("ROOM");
        roomServerIP = (String) savedUserInfo.get("IP");
        phone = (String) savedUserInfo.get("PHONE");
        txtRoom.setText(roomNumber + "호");

        shimmer = new Shimmer();
        shimmer.start(txtShimmer);
        Toast.makeText(this, roomNumber + "호" + getString(R.string.welcome), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // NOTE : initialize() must be called on start time.
        naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        txtShimmer.setText(R.string.lumi_init);

        changeUINowMicOff();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // NOTE : release() must be called on stop time.
        naverRecognizer.getSpeechRecognizer().release();
        releaseMediaPlayer(mediaPlayer);
    }

    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public class RequestTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {
            String command = params[0];
            Log.d(TAG + "_NETWORK", "전송문자열:" + command);

            try {
                // Simulate network access.
                // Thread.sleep(2000);

                DatagramSocket socket = new DatagramSocket();
                InetAddress inetAddress = InetAddress.getByName(roomServerIP);

                byte[] buf = (command).getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, roomServerPORT);

                socket.send(packet);
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
           /*
            catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            */

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Log.d(TAG + "_NETWORK", "전송완료");
            } else {
                Log.d(TAG + "_NETWORK", "전송실패");
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    public class CheckOutTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(String... params) {
            String command = params[0];
            Log.d(TAG + "_NETWORK", "전송문자열:" + command);

            try {
                DatagramSocket socket = new DatagramSocket();
                InetAddress inetAddress = InetAddress.getByName(serverIP);

                byte[] buf = (command).getBytes();

                DatagramPacket packet = new DatagramPacket(buf, buf.length, inetAddress, serverPort);

                socket.send(packet);
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

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Log.d(TAG + "_NETWORK", "전송완료");
            } else {
                Log.d(TAG + "_NETWORK", "전송실패");
            }
        }

        @Override
        protected void onCancelled() {
        }
    }
}
