package com.zluo.blankmusicplayer;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

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
        MediaPlayer.OnCompletionListener {
    private GestureDetectorCompat gestureDetectorCompat;
    private Deque<File> songQueue;
    private Stack<File> playedSongs;
    private Random rand = new Random();
    private MediaPlayer currentSong;
    private MediaPlayer nextSong;
    private List<File> tempMusicList;
    private File[] kgMusicFiles;
    private int musicFilesLength;
    private File currentSongFile;
    private File nextSongFile;
    private boolean loopThisSong = false;
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gestureDetectorCompat = new GestureDetectorCompat(this,this);
        gestureDetectorCompat.setOnDoubleTapListener(this);

        songQueue = new LinkedList<>();
        playedSongs = new Stack<>();

        tempMusicList = new ArrayList<>();
        kgMusicFiles = getMusicFiles();
        for (File file : kgMusicFiles) {
            if (file.getAbsolutePath().endsWith(".mp3") ||
                    file.getAbsolutePath().endsWith(".flac")) {
                tempMusicList.add(file);
            }
        }
        musicFilesLength = tempMusicList.size();
        kgMusicFiles = new File[musicFilesLength];
        kgMusicFiles = tempMusicList.toArray(kgMusicFiles);

        //adding 10 song files to queue
        for (int i = 0; i < 5; i++) {
            songQueue.offer(kgMusicFiles[rand.nextInt(musicFilesLength)]);
        }
        //adding 5 songs to stack in case user wants to prevent empty stack error
        for (int i = 0; i < 5; i++) {
            playedSongs.push(kgMusicFiles[rand.nextInt(musicFilesLength)]);
        }

        screenWidth = this.getResources().getDisplayMetrics().widthPixels;
        screenHeight = this.getResources().getDisplayMetrics().heightPixels;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }
    @Override
    protected void onStart() {
        super.onStart();

        currentSong = new MediaPlayer();
        //nextSong = new MediaPlayer();
        currentSongFile = songQueue.poll();
        Log.d("songfileexist",currentSongFile.toString());


        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            Log.d("erroronstart", "error is in onSTART");
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
        playedSongs = null;
        songQueue = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
            songQueue.offer(kgMusicFiles[rand.nextInt(musicFilesLength)]);

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

        songQueue.offer(kgMusicFiles[rand.nextInt(musicFilesLength)]);

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
        //also right and left to switch songs
        //if happened on top of screen -- adjust screen color to black or white
        if (motionEvent.getY() < screenHeight*0.3 && motionEvent1.getY() <screenHeight*0.3) {
            //adjust the brightness
            Toast.makeText(this,"You hit right on",Toast.LENGTH_SHORT).show();
        } else if (Math.abs(v1) > Math.abs(v)) {
            //adjust volume
            Toast.makeText(this,"Adjusting volume",Toast.LENGTH_SHORT).show();
        } else {
            //next or last song
            if (motionEvent.getX() > motionEvent1.getX()) {
                Toast.makeText(this,"OnScroll next song",Toast.LENGTH_SHORT).show();
                playNextSong();
            } else {
                Toast.makeText(this,"OnScroll previous song",Toast.LENGTH_SHORT).show();
                playPreviousSong();
            }
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //next song or previous song
        //add in a minimum distance the user needs to fling
        if (Math.abs(v) > Math.abs(v1)) {
            if (v < 0) {
                Toast.makeText(this,"OnFling next song",Toast.LENGTH_SHORT).show();
                playNextSong();
            } else {
                Toast.makeText(this,"OnFling previous song",Toast.LENGTH_SHORT).show();
                playPreviousSong();
            }
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (currentSong.isPlaying()) {
            currentSong.pause();
        } else {
            currentSong.start();
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        loopThisSong = !loopThisSong;
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        Toast.makeText(this,"This song's on loop" + loopThisSong, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public File[] getMusicFiles() {
        File allStorageDir = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(),"kgmusic");
        File downloadedMusicDir = new File(allStorageDir, "download");
        return downloadedMusicDir.listFiles();
    }
}
