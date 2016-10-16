package net.net16.suvankar.oasis_v12.utils;

import android.os.AsyncTask;
import android.util.Log;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

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

    private static boolean shuffle = false;
    private static int[] indexArray;
    private static int indexArrayIndex = 0;


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
            indexArray = new int[length];
            for(int i=0; i<length; i++)
                indexArray[i] = i;
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
            /*if (isPrevCalled) {
                currPos++;
                isPrevCalled = false;
            }*/
            if(shuffle) {
                if(indexArrayIndex == indexArray.length-1){
                    indexArrayIndex = -1;
                }
                currPos = indexArray[++indexArrayIndex];
                Log.d("index","nex index="+currPos+", indexarrayindex="+indexArrayIndex);
                String filepath = musicLibrary.get(currPos);
                Log.d("Song",currPos+" - "+filepath);
                return new File(filepath);
            }

            if (currPos >= length-1) {
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
            if(shuffle) {
                if(indexArrayIndex == 0) {
                    indexArrayIndex = indexArray.length;
                }
                currPos = indexArray[--indexArrayIndex];
                String filepath = musicLibrary.get(currPos);
                Log.d("Song",currPos+" - "+filepath);
                return new File(filepath);
            }
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

    public static void setShuffle(boolean shuffle) {
        MusicLibraryController.shuffle = shuffle;
        indexArray = shuffleArray(indexArray);
        indexArrayIndex = 0;
        currPos = indexArray[indexArrayIndex];
    }

    public static boolean isShuffle() {
        return shuffle;
    }

    /**
     * Fisher–Yates shuffle
     */
    static int[] shuffleArray(int[] array) {
        Random rnd = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
        return array;
    }
}
