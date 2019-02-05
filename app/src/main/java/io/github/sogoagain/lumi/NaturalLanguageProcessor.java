package io.github.sogoagain.lumi;

import android.util.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class NaturalLanguageProcessor {
    // ACT1: TV, ACT2: 에어컨, ACT3: 전등
    private static final String[][] ACT = { {"전부", "모두", "싹"},
                                            {"티비", "TV", "tv", "텔레비전"},
                                            {"에어컨", "냉방"},
                                            {"조명", "불", "전등", "붉"}};

    private static final String[] ON_ACTION = {"켜", "시작", "키고", "히고", "틀어"};
    private static final String[] OFF_ACTION = {"끄", "꺼", "멈춰", "그만", "중지"};

    private static final String TAG = "LUMI_NLP";

    public UserCommand extractIntent(String sentence) {
        Map<Integer, String> objects = new TreeMap<>();
        Map<Integer, String> commands = new TreeMap<>();

        int i = 0;
        for(String[] acts: ACT) {
            for (String act : acts) {
                if (sentence.indexOf(act) > -1) {
                    objects.put(sentence.indexOf(act), "ACT" + i);
                    Log.d(TAG, act);
                }
            }
            i++;
        }

        for(String onAction: ON_ACTION) {
            if(sentence.indexOf(onAction) > -1) {
                commands.put(sentence.indexOf(onAction), "Y");
                Log.d(TAG, onAction);
            }
        }

        for(String offAction: OFF_ACTION) {
            if(sentence.indexOf(offAction) > -1) {
                commands.put(sentence.indexOf(offAction), "N");
                Log.d(TAG, offAction);
            }
        }

        if(objects.isEmpty()) {
            Log.d(TAG, "객체 없음");
            return null;
        }

        if(commands.isEmpty()) {
            Log.d(TAG, "명령어 없음");
            return null;
        }

        if(objects.size() != commands.size()) {
            Log.d(TAG, "객체와 명령의 매칭 불가");
            return null;
        }

        UserCommand userCommand = new UserCommand();

        Iterator<Integer> objectIterator = objects.keySet().iterator();
        Iterator<Integer> commandIterator = commands.keySet().iterator();

        while(objectIterator.hasNext()){
            Integer objectIndex = objectIterator.next();
            Integer commandIndex = commandIterator.next();

            userCommand.setCommand(objects.get(objectIndex), commands.get(commandIndex));
        }

        if(userCommand.getCommand().contains("ACT0")) {
            UserCommand allSwitchCommand = new UserCommand();

            String command = userCommand.getCommand();
            command = command.substring(command.length() - 2, command.length() - 1);
            allSwitchCommand.setCommand("ACT1", command);
            allSwitchCommand.setCommand("ACT2", command);
            allSwitchCommand.setCommand("ACT3", command);

            return allSwitchCommand;
        }

        return userCommand;
    }
}
