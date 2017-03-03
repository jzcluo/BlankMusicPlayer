package com.zluo.blankmusicplayer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.Toast;
import android.speech.SpeechRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener ,
        GestureDetector.OnDoubleTapListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, RecognitionListener{
    private GestureDetectorCompat gestureDetectorCompat;
    private Deque<File> songQueue;
    private Stack<File> playedSongs;
    private Random rand = new Random();
    private MediaPlayer currentSong;
    private MediaPlayer nextSong;
    private File[] allMusicFiles;
    private int musicFilesLength;
    private File currentSongFile;
    private File nextSongFile;
    private boolean loopThisSong = false;
    private int screenWidth;
    private int screenHeight;
    private TextClock clock;
    private View myLayout;
    private int colorIndex = 0;
    private String[] colorArray;
    private int maxVolume = 48;
    private int currentVolume;
    private float volumeInLog;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean hideClock = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setting rotation to portrait. doesnt make sense to be landscape anyways
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        gestureDetectorCompat = new GestureDetectorCompat(this,this);
        gestureDetectorCompat.setOnDoubleTapListener(this);

        songQueue = new LinkedList<>();
        playedSongs = new Stack<>();

        List<File> tempMusicList = getMusicFiles(Environment.getExternalStorageDirectory().getAbsolutePath());
        musicFilesLength = tempMusicList.size();
        allMusicFiles = new File[musicFilesLength];
        allMusicFiles = tempMusicList.toArray(allMusicFiles);

        //adding 5 song files to queue
        for (int i = 0; i < 5; i++) {
            songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);
        }
        //adding 20 songs to stack in case user wants to prevent empty stack error
        for (int i = 0; i < 20; i++) {
            playedSongs.push(allMusicFiles[rand.nextInt(musicFilesLength)]);
        }

        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        screenHeight = this.getResources().getDisplayMetrics().heightPixels;

        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //setting clock and its color(White by default)
        clock = (TextClock)findViewById(R.id.textClock);
        clock.setTextColor(Color.BLACK);
        clock.setTextSize(screenWidth*0.1f);

        //setting my layout to the current one
        myLayout = findViewById(R.id.activity_main);

        //create color array. have to hard code this for now as i do not know a solution
        colorArray = new String[]{"#FAFAFA","#F5F5F5","#EEEEEE","#E0E0E0","#BDBDBD"
                ,"#9E9E9E","#757575","#616161","#424242","#212121"};

        //setting starting volume
        currentVolume = 36;
        volumeInLog = 1 - (float)(Math.log(maxVolume - currentVolume)/Math.log(maxVolume));

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,"en-US");
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,500);
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,Long.valueOf(3600000));


    }
    @Override
    protected void onStart() {
        super.onStart();

        currentSong = new MediaPlayer();
        currentSong.setVolume(volumeInLog,volumeInLog);
        //nextSong = new MediaPlayer();
        currentSongFile = songQueue.poll();

        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentSong.setOnPreparedListener(MainActivity.this);
        currentSong.setOnCompletionListener(MainActivity.this);

        currentSong.prepareAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentSong.start();
        currentSong.setOnCompletionListener(MainActivity.this);

        prepareNextSong();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
    }



    @Override
    protected void onPause() {
        super.onPause();

        currentSong.pause();

    }

    @Override
    protected void onStop() {
        super.onStop();

        currentSong.release();
        currentSong = null;
        if (nextSong != null) {
            nextSong.release();
            nextSong = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playedSongs = null;
        songQueue = null;
        speechRecognizer.destroy();
    }


    //speech Recognition implementation

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onResults(Bundle bundle) {
        if (bundle != null && bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
            List<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (results.size() != 0 && results.get(0).toLowerCase().equals("start")) {
                currentSong.start();
            }
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }

    //Setting methods for music player
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        if (loopThisSong) {
            currentSong.start();
        } else {
            //start playing next song
            nextSong.prepareAsync();

            //Push currentsong to the played stack and setting current the next song that'll play
            playedSongs.push(currentSongFile);
            currentSongFile = nextSongFile;

            //Releasing current song and setting it to the next song
            currentSong.release();
            currentSong = nextSong;
            //now nextsong is null. can't release and reset because currentSong points to the same
            nextSong = null;
            //adding a new song file onto the queue
            songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);

            //get next song
            prepareNextSong();
        }
    }

    //self-defined methods for playing last or next song
    public void playNextSong() {
        //push this song to the played song stack and getting next song to this song
        playedSongs.push(currentSongFile);
        currentSongFile = nextSongFile;
        currentSong.release();
        currentSong = nextSong;
        //already set on prepared listener
        currentSong.prepareAsync();

        nextSong = null;

        prepareNextSong();

        songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);

    }

    private void playPreviousSong() {
        songQueue.offerFirst(nextSongFile);
        songQueue.offerFirst(currentSongFile);

        currentSong.release();
        currentSong = null;

        currentSong = new MediaPlayer();

        currentSongFile = playedSongs.pop();
        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            Log.d("errorplayprevioussong","Error is in play previous song");
            e.printStackTrace();
        }
        currentSong.setOnPreparedListener(MainActivity.this);
        currentSong.prepareAsync();

        nextSong.release();
        nextSong = null;

        prepareNextSong();
    }

    private void prepareNextSong() {
        nextSong = new MediaPlayer();
        nextSongFile = songQueue.poll();
        try {
            nextSong.setDataSource(nextSongFile.getAbsolutePath());
        } catch (IOException e) {
            //maybe push the file back
            Log.d("errorpreparenextSong","Error in prepare next song");
            e.printStackTrace();
        }
        nextSong.setOnPreparedListener(MainActivity.this);
        nextSong.setOnCompletionListener(MainActivity.this);
        nextSong.setVolume(volumeInLog,volumeInLog);
    }


    //Setting methods for touch events
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        Log.d("location","X:" + motionEvent.getX() + "  Y: "+motionEvent.getY());
        Log.d("screen size","X:" + screenWidth + "  Y: "+screenHeight);

        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        //method moved to onsingletapconfirmed as double tap was made possible
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //up and down to adjust volume
        //if happened on top of screen -- adjust screen color to black or white
        if (motionEvent.getY() < screenHeight*0.3 && motionEvent1.getY() <screenHeight*0.3) {
            //adjust the brightness
            //Toast.makeText(this,"You hit right on",Toast.LENGTH_SHORT).show();
            if (v < 0) {
                if (colorIndex < 9) {
                    colorIndex += 1;
                }
                myLayout.setBackgroundColor(Color.parseColor(colorArray[colorIndex]));
            } else if (colorIndex >0) {
                colorIndex -= 1;
                myLayout.setBackgroundColor(Color.parseColor(colorArray[colorIndex]));
            }
            if (hideClock) {
                clock.setTextColor(Color.parseColor(colorArray[colorIndex]));
            } else {
                clock.setTextColor(Color.parseColor(colorArray[9 - colorIndex]));
            }

        } else if (Math.abs(v1) > Math.abs(v)) {
            if (v1 > 0) {
                //volume up
                if (currentVolume < maxVolume) {
                    currentVolume += 1;
                }
            } else if (currentVolume > 0) {
                currentVolume -=1;
            }
            if (currentVolume != maxVolume) {
                volumeInLog = 1 - (float) (Math.log(maxVolume - currentVolume) / Math.log(maxVolume));
                currentSong.setVolume(volumeInLog, volumeInLog);
            } else {
                currentSong.setVolume(1,1);
            }
            nextSong.setVolume(volumeInLog,volumeInLog);
        }
        //actually shouldn't put nextsongprevioussong here it will skip too many songs just let onfling work
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        hideClock = !hideClock;
        clock.setTextColor(Color.parseColor(colorArray[hideClock ? colorIndex : 9 - colorIndex]));
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //next song or previous song
        //add in a minimum distance the user needs to fling
        if (motionEvent.getY() > screenHeight*0.3 && motionEvent1.getY() > screenHeight*0.3) {
            if (Math.abs(v) > Math.abs(v1)) {
                if (v < 0) {
                    Toast.makeText(this, "OnFling next song", Toast.LENGTH_SHORT).show();
                    playNextSong();
                } else if (!playedSongs.isEmpty()) {
                    Toast.makeText(this, "OnFling previous song", Toast.LENGTH_SHORT).show();
                    playPreviousSong();
                }
            }
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (currentSong.isPlaying()) {
            currentSong.pause();
            //speechRecognizer.startListening(speechIntent);
        } else {
            currentSong.start();
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        loopThisSong = !loopThisSong;
        Toast.makeText(this,(loopThisSong? "" :"Not") + " On Loop", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    //recursively get all the mp3 files
    public ArrayList<File> getMusicFiles(String pathName) {
        Log.d("lookedFiles",pathName);
        ArrayList<File> allTheMusicFiles = new ArrayList<>();
        File allStorageDir = new File(pathName);
        File[] fileArray = allStorageDir.listFiles();
        if (fileArray != null && fileArray.length != 0) {
            for (File file : fileArray) {
                if (file.isDirectory()) {
                    allTheMusicFiles.addAll(getMusicFiles(file.getAbsolutePath()));
                } else if (file.getAbsolutePath().endsWith(".mp3") || file.getAbsolutePath().endsWith(".flac")) {
                    allTheMusicFiles.add(file);
                }
            }
        }
        return allTheMusicFiles;
    }
}
