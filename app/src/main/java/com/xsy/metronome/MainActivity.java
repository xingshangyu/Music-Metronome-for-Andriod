package com.xsy.metronome;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.Bundle;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

class PlayEvent extends TimerTask {
    MediaPlayer ding0, da0, ding1, da1;
    int rym, state = 8;
    boolean ding = false, da = false;

    PlayEvent(MediaPlayer player_ding0, MediaPlayer player_da0, MediaPlayer player_ding1,
              MediaPlayer player_da1, int rhyme) {
        ding0 = player_ding0;
        da0 = player_da0;
        ding1 = player_ding1;
        da1 = player_da1;
        rym = rhyme;
        if(rym == 0)
            state = Integer.MIN_VALUE;
    }

    @Override
    public void run() {
        if(++state >= rym) {
            state = 0;
            if(ding) {
                restart_player(ding0, ding1);
                ding = false;
            }
            else {
                restart_player(ding1, ding0);
                ding = true;
            }
        }
        else {
            if(da) {
                restart_player(da0, da1);
                da = false;
            }
            else {
                restart_player(da1, da0);
                da = true;
            }
        }
    }

    private void restart_player(MediaPlayer player_start, MediaPlayer player_stop) {
        player_start.start();
        player_stop.stop();
        try {
            player_stop.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    MediaPlayer ding0, da0, ding1, da1;
    Timer timer = null;
    boolean running = false;
    String filename = "mydata";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ding0 = MediaPlayer.create(getApplicationContext(), R.raw.ding);
        da0 = MediaPlayer.create(getApplicationContext(), R.raw.da);
        ding1 = MediaPlayer.create(getApplicationContext(), R.raw.ding);
        da1 = MediaPlayer.create(getApplicationContext(), R.raw.da);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        Pair<Integer, Integer> info = load();
        int velocity = info.first;
        int meter = info.second;
        EditText editText = (EditText) findViewById(R.id.velocity);
        editText.setText(String.valueOf(velocity));
        SeekBar seekbar = (SeekBar) findViewById(R.id.meter);
        seekbar.setProgress(meter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ding0.release();
        ding1.release();
        da0.release();
        da1.release();
        EditText editText = (EditText) findViewById(R.id.velocity);
        String message = editText.getText().toString();
        int velocity = Integer.parseInt(message);
        SeekBar seekbar = (SeekBar) findViewById(R.id.meter);
        int meter = seekbar.getProgress();
        save(velocity, meter);
    }

    public void toggle_button(View view) {
        if(running) stop();
        else    start();
    }

    private void int2bytes(int num, byte[] bytes) {
        bytes[3] = (byte) (num & 0xff);
        bytes[2] = (byte) ((num >> 8) & 0xff);
        bytes[1] = (byte) ((num >> 16) & 0xff);
        bytes[0] = (byte) ((num >> 24) & 0xff);
    }

    private void save(int vel, int rhyme) {
        Context context = getApplicationContext();
        File file = new File(context.getFilesDir(), filename);
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            byte[] buffer = new byte[4];
            int2bytes(vel, buffer);
            fos.write(buffer);
            int2bytes(rhyme, buffer);
            fos.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pair<Integer, Integer> load() {
        Context context = getApplicationContext();
        File file = new File(context.getFilesDir(), filename);
        int vel = 76, rhyme = 4;
        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = context.openFileInput(filename);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            DataInputStream din = new DataInputStream(fis);
            try {
                vel = din.readInt();
                rhyme = din.readInt();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Pair<>(vel, rhyme);
    }

    public void start() {
        EditText editText = (EditText) findViewById(R.id.velocity);
        String message = editText.getText().toString();
        int velocity = Integer.parseInt(message);
        SeekBar seekbar = (SeekBar) findViewById(R.id.meter);
        int meter = seekbar.getProgress();
        save(velocity, meter);
        play(velocity, meter);
        Button button = (Button) findViewById(R.id.button);
        button.setText(R.string.stop);
        running = true;
    }

    private void play(int vel, int rym) {
        if(vel > 400) {
            Toast.makeText(getApplicationContext(), "too fast!", Toast.LENGTH_LONG).show();
            stop();
            return;
        }
        timer = new Timer();
        timer.schedule(new PlayEvent(ding0, da0, ding1, da1, rym), 0, (long) 60000 / vel);
    }

    private void stop() {
        if(timer != null)   timer.cancel();
        timer = null;
        Button button = (Button) findViewById(R.id.button);
        button.setText(R.string.start);
        running = false;
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String preset = parent.getItemAtPosition(pos).toString();
        int tempo;
        switch(preset) {
            case "Grave":   tempo = 40; break;
            case "Largo":   tempo = 46; break;
            case "Adagio":   tempo = 56; break;
            case "Larghetto":   tempo = 60; break;
            case "Andante":   tempo = 66; break;
            case "Andantino":   tempo = 69; break;
            case "Moderato":   tempo = 88; break;
            case "Allegretto":   tempo = 108; break;
            case "Allegro":   tempo = 132; break;
            case "Vivace":   tempo = 152; break;
            case "Presto":   tempo = 184; break;
            case "Prestissimo":   tempo = 208; break;
            default:    return;
        }
        EditText editText = (EditText) findViewById(R.id.velocity);
        editText.setText(String.valueOf(tempo));
    }
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }
}
