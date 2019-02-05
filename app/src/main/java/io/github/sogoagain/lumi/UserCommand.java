package io.github.sogoagain.lumi;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class UserCommand {
    private static final String TAG = UserCommand.class.getSimpleName();
    // Key 객체, Value 명령어
    private Map<String, String> userCommands = new HashMap<>();

    public void setCommand(String object, String command) {
        userCommands.put(object, command);
    }

    public String getCommand() {
        String result = "";

        for( String key : userCommands.keySet() ){
            result += key + ":" + userCommands.get(key) + " ";
            Log.d(TAG,key + ":" + userCommands.get(key));
        }

        Log.d(TAG, result);
        return result;
    }
}
