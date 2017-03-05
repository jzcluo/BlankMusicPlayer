package com.zluo.blankmusicplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
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
        MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener,
        RecognitionListener{
    private GestureDetectorCompat gestureDetectorCompat;
    private Deque<File> songQueue;
    private Stack<File> playedSongs;
    private Random rand = new Random();
    private MediaPlayer currentSong;
    private MediaPlayer nextSong;
    private List<File> tempMusicList;
    private File[] allMusicFiles;
    private int musicFilesLength;
    private File currentSongFile;
    private File nextSongFile;
    private boolean loopThisSong = false;
    private int screenWidth;
    private int screenHeight;
    private TextClock clock;
    private View myLayout;
    private int colorIndex;
    private String[] colorArray;
    private int maxVolume = 48;
    private int currentVolume;
    private float volumeInLog;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private boolean hideClock = false;
    private AudioManager audioManager;
    private int streamVolumeMusic;
    private int streamVolumeSystem;
    private boolean allowVoiceControl = false;
    private boolean volumeMutedByThisApp = false;
    private int currentPosition;
    private final String PREFERENCE_FILE_KEY = "com.zluo.BlankMusicPlayer.PREFERENCE_FILE_KEY";
    private final String PLAYBACK_POSITION = "PLAYBACK_POSITION";
    private final String SONG_FILE = "SONG_FILE";
    private final String COLOR_INDEX = "COLOR_INDEX";
    private final String CLOCK_COLOR = "CLOCK_COLOR";
    private final String ON_VOICE_CONTROL = "ON_VOICE_CONTROL";
    private final String CURRENT_VOLUME = "CURRENT_VOLUME";
    private final String FILES_STRING = "FILES_STRING";
    private String stringOfFiles;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;


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

        //saving data
        sharedPreferences = this.getSharedPreferences(PREFERENCE_FILE_KEY,MODE_PRIVATE);
        editor = sharedPreferences.edit();

        stringOfFiles = sharedPreferences.getString(FILES_STRING, "");

        if (stringOfFiles.length() == 0) {
            tempMusicList = getMusicFiles(Environment.getExternalStorageDirectory().getAbsolutePath());
            musicFilesLength = tempMusicList.size();
            allMusicFiles = new File[musicFilesLength];
            allMusicFiles = tempMusicList.toArray(allMusicFiles);
            editor.putString(FILES_STRING, musicFilesToString(allMusicFiles));
            editor.apply();
        } else {
            String[] stringFilesArray = stringOfFiles.split("\\s+");
            musicFilesLength = stringFilesArray.length;
            Log.d("filelength"," "+musicFilesLength);
            allMusicFiles = new File[musicFilesLength];
            for (int i = 0; i < musicFilesLength; i++) {
                allMusicFiles[i] = new File(stringFilesArray[i]);
            }
        }

        //adding 5 song files to queue
        for (int i = 0; i < 5; i++) {
            songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);
        }
        //adding 20 songs to stack in case user swipes left. prevent empty stack error
        for (int i = 0; i < 20; i++) {
            playedSongs.push(allMusicFiles[rand.nextInt(musicFilesLength)]);
            Log.d("push","push in oncreate");
        }

        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        screenHeight = this.getResources().getDisplayMetrics().heightPixels;

        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //create color array. have to hard code this for now as i do not know a solution
        colorArray = new String[]{"#FAFAFA","#F5F5F5","#EEEEEE","#E0E0E0","#BDBDBD"
                ,"#9E9E9E","#757575","#616161","#424242","#212121"};



        //setting clock and its color(White by default)
        clock = (TextClock)findViewById(R.id.textClock);
        clock.setTextColor(Color.parseColor(colorArray[sharedPreferences.getInt(CLOCK_COLOR,9)]));
        clock.setTextSize(screenWidth*0.1f);

        colorIndex = sharedPreferences.getInt(COLOR_INDEX,0);
        //setting my layout to the current one
        myLayout = findViewById(R.id.activity_main);
        myLayout.setBackgroundColor(Color.parseColor(colorArray[colorIndex]));

        allowVoiceControl = sharedPreferences.getBoolean(ON_VOICE_CONTROL,false);

        //setting starting volume
        currentVolume = sharedPreferences.getInt(CURRENT_VOLUME, 38);
        volumeInLog = 1 - (float)(Math.log(maxVolume - currentVolume)/Math.log(maxVolume));

        //set up speech intent
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,"en-US");
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,500);


        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("onstart","on start called" + currentPosition);
        currentSong = new MediaPlayer();
        currentSong.setVolume(volumeInLog,volumeInLog);
        //nextSong = new MediaPlayer();

        //getting data from sharedPreference. clearing data right after;
        currentSongFile = new File(sharedPreferences.getString(SONG_FILE,songQueue.poll().getAbsolutePath()));
        currentPosition = sharedPreferences.getInt(PLAYBACK_POSITION,0);
        editor.remove(PLAYBACK_POSITION).apply();
        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentSong.setOnPreparedListener(MainActivity.this);
        currentSong.setOnCompletionListener(MainActivity.this);
        currentSong.setOnSeekCompleteListener(MainActivity.this);


        //currentSong.prepareAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onresume","on resume called");
        //putting this line here because can use seek to
        currentSong.prepareAsync();
        //onResume is always called with onStart therefore no need to start
        //currentSong.start();
        //currentSong.setOnCompletionListener(MainActivity.this);
        Log.d("queuelength"," "+songQueue.size());
        Log.d("stacklength"," "+playedSongs.size());
        prepareNextSong();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("onrestart","on restart called");
    }



    @Override
    protected void onPause() {
        super.onPause();
        Log.d("onpause","on pause called");
        //saving data
        //editor = sharedPreferences.edit();
        currentSong.stop();

        if (volumeMutedByThisApp) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolumeSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            speechRecognizer.destroy();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("onstop","on stop called" + currentPosition);
        //currentPosition = currentSong.getCurrentPosition();
        if (volumeMutedByThisApp) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolumeSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            speechRecognizer.destroy();
        }
        editor.putInt(PLAYBACK_POSITION, currentSong.getCurrentPosition());
        editor.putString(SONG_FILE, currentSongFile.getAbsolutePath());
        editor.putInt(COLOR_INDEX, colorIndex);
        editor.putInt(CLOCK_COLOR, hideClock ? colorIndex : 9 - colorIndex);
        editor.putBoolean(ON_VOICE_CONTROL, allowVoiceControl);
        editor.putInt(CURRENT_VOLUME, currentVolume);
        editor.apply();
        currentSong.release();
        currentSong = null;
        if (nextSong != null) {
            nextSong.release();
            nextSong = null;
        }
        //need to add two songs to songQueue because two songs are wasted here.Otherwise null pointer
        //oncompletion and play next song would have added two
        songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);
        songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playedSongs = null;
        songQueue = null;
/*
        Log.d("ondestroy","on destroy called");
        editor.remove(PLAYBACK_POSITION);
        editor.remove(SONG_FILE);
        tempMusicList = getMusicFiles(Environment.getExternalStorageDirectory().getAbsolutePath());
        musicFilesLength = tempMusicList.size();
        allMusicFiles = new File[musicFilesLength];
        allMusicFiles = tempMusicList.toArray(allMusicFiles);
        stringOfFiles = musicFilesToString(allMusicFiles);
        editor.putString(FILES_STRING, stringOfFiles);
        editor.apply();
        Log.d("ondestroy","on destroy called");
        */
    }


    //speech Recognition implementation

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.d("readyforspeech","Im ready for speech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("beginningofspeech","Im beginning of speech");
    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {
        Log.d("endofspeech","Im at end of speech");
    }

    @Override
    public void onError(int i) {
        speechRecognizer.destroy();
        if (!currentSong.isPlaying()){

            //Worked when you destroy the speechrecognizer and reinstantiate it again
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
            speechRecognizer.setRecognitionListener(MainActivity.this);
            speechRecognizer.startListening(speechIntent);
            Log.d("onerrorcalled", "Im called in on error");
        }
    }

    @Override
    public void onResults(Bundle bundle) {
        speechRecognizer.destroy();
        if (!currentSong.isPlaying()) {
            if (bundle != null && bundle.containsKey(SpeechRecognizer.RESULTS_RECOGNITION)) {
                List<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (results.size() != 0 && results.get(0).toLowerCase().equals("start")) {
                    //speechRecognizer.destroy();
                    currentSong.start();
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolumeSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    return;
                }
            }
            Log.d("amicalled", "i am called before start listening in onresults!");
            //cancel gives a vibrate where destroy doesnt
            //speechRecognizer.destroy();
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
            speechRecognizer.setRecognitionListener(MainActivity.this);
            speechRecognizer.startListening(speechIntent);
            Log.d("listeningfromonresults", "im listening from on results");
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
        if (currentPosition != 0) {
            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                }
            });
            mediaPlayer.seekTo(currentPosition);
            currentPosition = 0;
        } else {
            mediaPlayer.start();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
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
            Log.d("push","push in oncompleion");
            currentSongFile = nextSongFile;

            //Releasing current song and setting it to the next song
            currentSong.release();
            currentSong = null;

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
        Log.d("length","play next song");
        Log.d("stacklength"," "+playedSongs.size());

        //push this song to the played song stack and getting next song to this song
        playedSongs.push(currentSongFile);
        Log.d("push","push in playnextsong");
        currentSongFile = nextSongFile;
        currentSong.release();
        currentSong = null;

        currentSong = nextSong;
        //already set on prepared listener
        currentSong.prepareAsync();

        nextSong = null;

        prepareNextSong();

        songQueue.offer(allMusicFiles[rand.nextInt(musicFilesLength)]);

    }

    private void playPreviousSong() {
        Log.d("length","play previous song");
        Log.d("length"," "+songQueue.size());

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
        //currentSong.setOnCompletionListener(MainActivity.this);
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
        if (motionEvent.getY() < screenHeight*0.3) {
            hideClock = !hideClock;
            clock.setTextColor(Color.parseColor(colorArray[hideClock ? colorIndex : 9 - colorIndex]));
        } else {
            if (allowVoiceControl) {
                try {
                    speechRecognizer.destroy();
                } catch (NullPointerException e) {

                }
                if (volumeMutedByThisApp) {
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolumeSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                }
            }
            allowVoiceControl = !allowVoiceControl;
            Toast.makeText(MainActivity.this, "Voice control "
                    + (allowVoiceControl ? "en" : "dis") + "abled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //next song or previous song
        //add in a minimum distance the user needs to fling
        if (motionEvent.getY() > screenHeight*0.3 && motionEvent1.getY() > screenHeight*0.3) {
            if (Math.abs(v) > Math.abs(v1)) {
                if (v < 0) {
                    //Toast.makeText(this, "OnFling next song", Toast.LENGTH_SHORT).show();
                    playNextSong();
                } else if (!playedSongs.isEmpty()) {
                    //Toast.makeText(this, "OnFling previous song", Toast.LENGTH_SHORT).show();
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
            streamVolumeMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            streamVolumeSystem = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            if (allowVoiceControl) {
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                speechRecognizer.setRecognitionListener(MainActivity.this);
                speechRecognizer.startListening(speechIntent);
                Log.d("listeningfromsingletap", "im listening from singletap");
                volumeMutedByThisApp = true;
            }
        } else {
            if (allowVoiceControl) {
                //calling destroy in this method causes the phone to vibrate(no idea why)
                speechRecognizer.stopListening();
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolumeSystem, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
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
    private ArrayList<File> getMusicFiles(String pathName) {
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

    private String musicFilesToString(File[] filesArray) {
        StringBuilder builder = new StringBuilder();
        for (File file : filesArray) {
            builder.append(file.getAbsolutePath()).append(" ");
        }
        //builder.setLength(builder.length() - 1);
        return builder.toString();
    }
}
