package net.net16.suvankar.oasis_v12.utils;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suvankarmitra on 9/10/2016.
 */
public class MusicLibraryController {
    //private static MediaPlayer mediaPlayer;
    private static ArrayList<String> musicLibrary;
    private static Boolean musicReadComplete = false;

    private static String prev = null;
    private static int currPos = 0;
    private static int length = 0;
    private static boolean isPrevCalled = false;

    private MusicLibraryController(){
    }

    public static void setMusicReadComplete(Boolean musicReadComplete) {
        MusicLibraryController.musicReadComplete = musicReadComplete;
    }

    @Contract(pure = true)
    public static boolean isMusicReadComplete() {
        return musicReadComplete;
    }

    /*public static MediaPlayer getMediaPlayer() {
        if(mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        return mediaPlayer;
    }*/

    public static void setMusicLibrary(String root) {
        File file = new File(root);
        setMusicLibrary(file);
    }

    public static ArrayList<String> getMusicLibrary() {
        synchronized (musicLibrary){
            while(!musicReadComplete)
                try {
                    musicLibrary.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return musicLibrary;
        }
    }

    public static void setMusicLibrary(final File root) {
        musicLibrary = new ArrayList<>();
        //read music files from storage
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                musicReadComplete = readAllMusicFromStorage(root);

            }
        });
    }

    private static boolean readAllMusicFromStorage(File root) {
        synchronized (musicLibrary) {
            if(root != null ){
                if(root.isDirectory()) {
                    //Log.d("DIR",root.getAbsolutePath());
                    File[] files = root.listFiles();
                    if(files!= null) {
                        for(File f : files) {
                            readAllMusicFromStorage(f);
                        }
                    }
                }
                else {
                    if(root.isFile() && root.getName().toLowerCase().endsWith("mp3")) {
                        //Log.d("FILE",root.getAbsolutePath());
                        musicLibrary.add(root.getAbsolutePath());
                    }
                }
            }
            length = musicLibrary.size();
            musicLibrary.notify();
            musicReadComplete = true;
            return musicReadComplete;
        }
    }

    public static File next() {
        synchronized (musicLibrary) {
            while(!musicReadComplete)
                try {
                    musicLibrary.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            if (isPrevCalled) {
                currPos++;
                isPrevCalled = false;
            }
            if (currPos >= length) {
                currPos = 0;
            }
            String filepath = musicLibrary.get(++currPos);
            Log.d("Song",currPos+" - "+filepath);
            return new File(filepath);
        }
    }

    public static File previous() {
        synchronized (musicLibrary) {
            while(!musicReadComplete)
                try {
                    musicLibrary.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            isPrevCalled = true;
            if (currPos == 0) {
                prev = musicLibrary.get(length - 1);
                currPos = length - 1;
            } else {
                prev = musicLibrary.get(--currPos);
            }
            Log.d("Song",currPos+" - "+prev);
            return new File(prev);
        }
    }

    public static int getIndex() {
        return currPos;
    }

    public static void setIndex(int index) {
        if(index<0 || index >= musicLibrary.size())
            index = 0;
        currPos = index;
    }

    public static File getFile(int index) {
        if(index<0 || index >= musicLibrary.size())
            index = 0;
        currPos = index;
        return new File(musicLibrary.get(index));
    }

    @Contract(pure = true)
    public static boolean hasNext() {
        return currPos<length && currPos>=0;
    }


    @Contract(" -> !null")
    public static File current() {
        synchronized (musicLibrary) {
            while(!musicReadComplete)
                try {
                    musicLibrary.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return new File(musicLibrary.get(currPos));
        }
    }
}
