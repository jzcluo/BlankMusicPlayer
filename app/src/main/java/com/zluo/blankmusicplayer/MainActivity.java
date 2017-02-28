package com.zluo.blankmusicplayer;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener ,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener{
    private GestureDetectorCompat gestureDetectorCompat;
    private Deque<File> songQueue;
    private Stack<File> playedSongs;
    private Random rand = new Random();
    private MediaPlayer currentSong;
    private MediaPlayer nextSong;
    private File[] kgMusicFiles;
    private int musicFilesLength;
    private File currentSongFile;
    private File nextSongFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gestureDetectorCompat = new GestureDetectorCompat(this,this);

        songQueue = new LinkedList<>();
        playedSongs = new Stack<>();

        kgMusicFiles = getMusicFiles();
        musicFilesLength = kgMusicFiles.length;

        //adding 10 song files to queue
        for (int i = 0; i < 10; i++) {
            songQueue.offer(kgMusicFiles[rand.nextInt(musicFilesLength)]);
        }

    }
    @Override
    protected void onStart() {
        super.onStart();

        currentSong = new MediaPlayer();
        nextSong = new MediaPlayer();
        currentSongFile = songQueue.poll();
        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            Log.i("erroronstart","error is in onStart");
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
    }



    //Setting methods for music player
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        //start playing next song
        nextSong.prepareAsync();

        //Push currentsong to the played stack and setting current the next song that'll play
        playedSongs.push(currentSongFile);
        currentSongFile = nextSongFile;

        //Releasing current song and setting it to the next song
        currentSong.release();
        currentSong = nextSong;
        //now nextsong is null
        //nextSong = null;
        nextSong.release();
        //adding a new song file onto the queue
        songQueue.offer(kgMusicFiles[rand.nextInt(musicFilesLength)]);

        //get next song
        prepareNextSong();

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

        prepareNextSong();

    }

    private void playPreviousSong() {
        songQueue.offerFirst(nextSongFile);
        songQueue.offerFirst(currentSongFile);

        currentSong.release();

        currentSongFile = playedSongs.pop();
        try {
            currentSong.setDataSource(currentSongFile.getAbsolutePath());
        } catch (IOException e) {
            Log.i("errorplayprevioussong","Error is in play previous song");
            e.printStackTrace();
        }
        currentSong.setOnPreparedListener(MainActivity.this);
        currentSong.prepareAsync();

        nextSong.release();
        //nextSong = null;
        prepareNextSong();
    }

    private void prepareNextSong() {
        nextSongFile = songQueue.poll();
        try {
            nextSong.setDataSource(nextSongFile.getAbsolutePath());
        } catch (IOException e) {
            //maybe push the file back
            e.printStackTrace();
        }
        nextSong.setOnPreparedListener(MainActivity.this);
        nextSong.setOnCompletionListener(MainActivity.this);
    }




    //Setting methods for touch events
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        //pause or start the music depending on the status
        if (currentSong.isPlaying()) {
            currentSong.pause();
        } else {
            currentSong.start();
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //up and down to adjust volume
        //also right and left to switch songs
        //if happened on top of screen -- adjust screen color to black or white


        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        //next song or previous song
        if (motionEvent.getX() > motionEvent.getX()) {
            playNextSong();
        } else {
            playPreviousSong();
        }
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
        File[] kgMusicFiles = downloadedMusicDir.listFiles();
        return kgMusicFiles;
    }
}
