package net.net16.suvankar.oasis_v12;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import net.net16.suvankar.oasis_v12.utils.MusicLibraryController;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private final String root = "/storage/";
    private String PACKAGE_NAME;

    //Music Player service
    private static MediaPlayerService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;

    //Song list
    ArrayList<String> songList;

    //control buttons
    static ImageButton _play;
    ImageButton next;
    ImageButton prev;
    ImageButton queue;
    ImageButton fav;
    ImageButton navDrawer;
    ImageButton share;
    static SeekBar _seekBar;
    static TextView _curTime;
    static TextView _totTime;
    static TextView _songTitle;
    static ImageView _albumArt;
    //animations
    Animation playButtonAnimation;
    Animation queueButtonAnimation;

    //notification custom view
    private static RemoteViews notifView;

    //notification control button
    private ImageButton notifPlay;
    private ImageButton notifNext;

    private static boolean _applicationRunning = true;

    private static int _currentPos = 0;
    private static boolean _touchingSeekBar = false;
    private static boolean _isPlaying = false;  // This variable will track the current state of the player
    // It is very important to properly adjust this variable for
    // play pause next previous options.

    //headset receiver
    private HeadsetIntentReceiver headsetIntentReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _applicationRunning = true;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        PACKAGE_NAME = getApplicationContext().getPackageName();

        //requesting user permission to access storage
        getPermissionFromUser();

        //initialize variables
        initializeVariables();

        if (musicSrv != null) {
            if (musicSrv.isPlaying()) {
                _play.setImageResource(R.drawable.ic_pause_black_24dp);
            } else {
                _play.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            }
        }

    }

    private void initializeVariables() {
        //initialize local variables
        _songTitle = (TextView) findViewById(R.id.songTitle);
        _play = (ImageButton) findViewById(R.id.play);
        next = (ImageButton) findViewById(R.id.next);
        prev = (ImageButton) findViewById(R.id.prev);
        queue = (ImageButton) findViewById(R.id.queue);
        navDrawer = (ImageButton) findViewById(R.id.navDrawerButton);
        fav = (ImageButton) findViewById(R.id.fav);
        share = (ImageButton) findViewById(R.id.share);
        _curTime = (TextView) findViewById(R.id.currentTime);
        _totTime = (TextView) findViewById(R.id.totalTime);
        _seekBar = (SeekBar) findViewById(R.id.seekBar);
        _albumArt = (ImageView) findViewById(R.id.albumArt);

        notifPlay = (ImageButton) findViewById(R.id.noti_playButton);
        notifNext = (ImageButton) findViewById(R.id.noti_nextButton);

        playButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.play_button_anim_scale);
        queueButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.music_queue_anim_scale);

        //init notification manager
        notifView = new RemoteViews(PACKAGE_NAME, R.layout.custom_notification_view);
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new Notification.Builder(getApplicationContext())
                .setContentTitle("Playing music")
                .setSmallIcon(R.drawable.ic_play_circle_filled_black_24dp)
                .setContentIntent(pendingIntent);

        defaultAlbumArtBitmap = BitmapFactory.decodeResource(getBaseContext().getResources(), R.drawable.album_art);

        headsetIntentReceiver = new HeadsetIntentReceiver();
    }

    private void addOnclickListeners() {
        _play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(playButtonAnimation);
                if (!_isPlaying) {
                    //musicSrv.playSong();
                    musicSrv.playSong(MusicLibraryController.current());
                    _play.setImageResource(R.drawable.ic_pause_black_24dp);
                    _isPlaying = true;
                    showNotification(musicSrv.nowPlaying);
                } else {
                    musicSrv.pauseSong();
                    _play.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                    _isPlaying = false;
                    showNotification(musicSrv.nowPlaying);
                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(playButtonAnimation);
                //musicSrv.playNextSong();
                musicSrv.playNextSong(MusicLibraryController.next());
                _isPlaying = true;
                _play.setImageResource(R.drawable.ic_pause_black_24dp);
                showNotification(musicSrv.nowPlaying);
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(playButtonAnimation);
                //musicSrv.playPrevSong();
                musicSrv.playPrevSong(MusicLibraryController.previous());
                _isPlaying = true;
                _play.setImageResource(R.drawable.ic_pause_black_24dp);
                //showNotification(musicSrv.nowPlaying);
            }
        });

        queue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(queueButtonAnimation);
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction tx = fm.beginTransaction();
                tx.setCustomAnimations(R.anim.frag_enter_from_right, R.anim.frag_exit_to_left,
                        R.anim.frag_enter_from_left, R.anim.frag_exit_to_right);

                //addToBackStack is very important here
                //tx.add(R.id.mainActivity, new SongListFragment()).addToBackStack("main fragment").commit();
                tx.add(android.R.id.content, new SongListFragment()).addToBackStack("main").commit();
            }
        });

        navDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction tx = fm.beginTransaction();
                tx.setCustomAnimations(R.anim.frag_enter_from_left, R.anim.frag_exit_to_right,
                        R.anim.frag_enter_from_right, R.anim.frag_exit_to_left);
                tx.add(android.R.id.content, new NavigationDrawerFragment()).addToBackStack("main").commit();
            }
        });

        _seekBar.setOnSeekBarChangeListener(musicSrv);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                File fileToShare = musicSrv.nowPlaying;
                if (fileToShare == null) {
                    return;
                }
                ContentResolver cr = getContentResolver();
                Uri uri = Uri.fromFile(fileToShare);
                sharingIntent.setType("audio/mpeg");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, fileToShare.getName());
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

    }

    public static MediaPlayerService getMusicSrv() {
        return musicSrv;
    }

    public static boolean is_isPlaying() {
        return _isPlaying;
    }

    //notification config
    private static NotificationManager manager;
    static Notification notification;
    static Notification.Builder builder;
    private static final int NOTOFICATION_ID = 4567;

    //these are intent actions to broadcast
    public static String NEXT = "next";
    public static String PLAY = "play";

    /**
     * @param nowPlaying
     */
    public void showNotification(final File nowPlaying) {
        notifView.setImageViewBitmap(R.id.noti_albumart, getAlbumArt(nowPlaying));
        notifView.setTextViewText(R.id.noti_title, getMediaTitle(nowPlaying));
        notifView.setTextViewText(R.id.noti_artist, getArtistName(nowPlaying));

        Intent playPauseIntent = new Intent(this, MusicBroadcastReceiver.class);
        playPauseIntent.setAction(PLAY);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notifView.setOnClickPendingIntent(R.id.noti_playButton, pendingIntent);

        Intent nextIntent = new Intent(this, MusicBroadcastReceiver.class);
        nextIntent.setAction(NEXT);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(getBaseContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notifView.setOnClickPendingIntent(R.id.noti_nextButton, pendingIntent1);

        if (_isPlaying) {
            notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_pause_black_24dp);
        } else {
            notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_play_arrow_black_24dp);
        }
        notification = builder.build();
        notification.contentView = notifView;
        manager.notify(NOTOFICATION_ID, notification);
    }

    /**
     * A broadcast receiver to receive notification broadcasts
     */
    public static class MusicBroadcastReceiver extends BroadcastReceiver {

        public MusicBroadcastReceiver() {

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BROADCAST", "broadcast received");

            String ac = intent.getAction();
            int code = -1;
            if (ac.equalsIgnoreCase(PLAY)) {
                code = 1;
            } else if (ac.equalsIgnoreCase(NEXT)) {
                code = 2;
            }
            Log.d("BROARCAST", "code= " + code + " playing=" + _isPlaying);
            switch (code) {
                case 1:
                    if (_isPlaying) {
                        musicSrv.pauseSong();
                        _play.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                        notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_play_arrow_black_24dp);
                        _isPlaying = false;
                    } else {
                        musicSrv.playSong();
                        _play.setImageResource(R.drawable.ic_pause_black_24dp);
                        notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_pause_black_24dp);
                        _isPlaying = true;
                    }

                    notifView.setImageViewBitmap(R.id.noti_albumart, getAlbumArt(musicSrv.nowPlaying));
                    notifView.setTextViewText(R.id.noti_title, getMediaTitle(musicSrv.nowPlaying));
                    notifView.setTextViewText(R.id.noti_artist, getArtistName(musicSrv.nowPlaying));
                    break;

                case 2:
                    musicSrv.playNextSong(MusicLibraryController.next());
                    _isPlaying = true;
                    _play.setImageResource(R.drawable.ic_pause_black_24dp);
                    notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_pause_black_24dp);
                    notifView.setImageViewBitmap(R.id.noti_albumart, getAlbumArt(musicSrv.nowPlaying));
                    notifView.setTextViewText(R.id.noti_title, getMediaTitle(musicSrv.nowPlaying));
                    notifView.setTextViewText(R.id.noti_artist, getArtistName(musicSrv.nowPlaying));
                    break;
            }
            notification = builder.build();
            notification.contentView = notifView;
            manager.notify(NOTOFICATION_ID, notification);
        }
    }

    /**
     *  Headset connect disconnect receiver
     */
    private class HeadsetIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d("Headset", "Headset is unplugged");
                        musicSrv.pauseSong();
                        _play.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                        notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_play_arrow_black_24dp);
                        _isPlaying = false;

                        break;
                    case 1:
                        Log.d("Headset", "Headset is plugged");
                        break;
                }
            }
        }
    }

    private static String getArtistName(File file) {
        if (file == null)
            return null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
        if (artist == null)
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        mmr.release();
        if (artist == null) {
            artist = "Unknown artist";
        }
        return artist;
    }

    private static String getMediaTitle(File file) {
        if (file == null)
            return null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title == null)
            title = file.getName();
        mmr.release();
        return title;
    }

    private static Bitmap defaultAlbumArtBitmap;

    private static Bitmap getAlbumArt(File file) {
        if (file == null)
            return null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        byte[] bytes = mmr.getEmbeddedPicture();
        mmr.release();
        Bitmap bitmap = null;
        if (bytes != null) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        if (bitmap == null) {
            bitmap = defaultAlbumArtBitmap;
        }
        return bitmap;
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("TAG", "service connected");
            MediaPlayerService.MusicBinder binder = (MediaPlayerService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();

            musicBound = true;

            //prepare album art and song title
            musicSrv.prepareForPlayback(MusicLibraryController.current());

            //add on click listeners
            addOnclickListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("TAG", "Start application");
        if (playIntent == null) {
            playIntent = new Intent(this, MediaPlayerService.class);
            startService(playIntent);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //registering HeadsetBroadcastReceiver
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetIntentReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        //unregistering HeadsetBroadcastReceiver
        unregisterReceiver(headsetIntentReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(musicConnection);
        stopService(playIntent);
        musicSrv = null;
        MusicLibraryController.setMusicReadComplete(false);
        //manager.cancel(NOTOFICATION_ID);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currPos", MusicLibraryController.getIndex());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        MusicLibraryController.setIndex(savedInstanceState.getInt("currPos"));
    }

    /**
     * This method override is very important otherwise
     * on back pressed the activity will be destroyed
     */
    @Override
    public void finish() {
        //super.finish();
        moveTaskToBack(true);
        _applicationRunning = true;
    }


    private void getPermissionFromUser() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "not granted");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed", "Rationale", Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            MusicLibraryController.setMusicLibrary(root);
            songList = MusicLibraryController.getMusicLibrary();
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("TAG", "onRequestPermission");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MusicLibraryController.setMusicLibrary(root);
                    songList = MusicLibraryController.getMusicLibrary();
                }
            }
        }
    }

    /**
     * Music player service class
     */
    public static class MediaPlayerService extends Service implements MediaPlayer.OnPreparedListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, SeekBar.OnSeekBarChangeListener, AudioManager.OnAudioFocusChangeListener {

        //media player
        private MediaPlayer player;
        private boolean playing = false;    //if true -> the service is started and running, the media player may be paused or running
        private boolean onPause = false;
        private int currentPlayerPosition = 0;
        private File nowPlaying;

        public File getNowPlaying() {
            return nowPlaying;
        }

        public boolean isPlaying() {
            return playing;
        }

        public boolean isOnPause() {
            return onPause;
        }

        //no argument constructor
        public MediaPlayerService() {

        }

        @Override
        public void onCreate() {
            Log.d("Service", "service created");
            super.onCreate();

            //creating audio focus
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d("AUDIO", "Audio focus not received");
                stopSelf();
            } else {
                Log.d("AUDIO", "Audio focus received");
            }

            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        while (_applicationRunning && player != null) {
                            if (player.isPlaying() && _isPlaying) {
                                //updating the current time
                                _currentPos = player.getCurrentPosition();
                                //updating seekbar
                                if (!_touchingSeekBar) {
                                    //Log.d("Seekbar",_currentPos+"");
                                    _seekBar.setProgress(_currentPos);
                                }
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                    } catch (IllegalStateException e) {
                        //the player has stopped

                    }
                }
            };

            if (timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }

            timer = new Timer(true);
            timer.scheduleAtFixedRate(timerTask, 0, 100);

            //create player
            player = new MediaPlayer();
            //set player properties
            initMusicPlayer();
        }

        public void initMusicPlayer() {
            //set player properties
            //player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            //updating the current time
            _currentPos = seekBar.getProgress();
            int dur = _currentPos;
            int min = dur / 1000 / 60;
            int sec = dur - min * 60 * 1000;
            sec /= 1000;
            if (sec <= 9)
                _curTime.setText(min + ":0" + sec);
            else
                _curTime.setText(min + ":" + sec);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            _touchingSeekBar = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            _touchingSeekBar = false;
            int pos = seekBar.getProgress();
            if (_isPlaying) {
                pauseSong();
            }

            currentPlayerPosition = pos;
            playSong(MusicLibraryController.current());
            _play.setImageResource(R.drawable.ic_pause_black_24dp);
            _isPlaying = true;
            showNotification(nowPlaying);
        }

        private void showNotification(final File nowPlaying) {
            notifView.setImageViewBitmap(R.id.noti_albumart, getAlbumArt(nowPlaying));
            notifView.setTextViewText(R.id.noti_title, getMediaTitle(nowPlaying));
            notifView.setTextViewText(R.id.noti_artist, getArtistName(nowPlaying));

            Intent playPauseIntent = new Intent(this, MusicBroadcastReceiver.class);
            playPauseIntent.setAction(PLAY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifView.setOnClickPendingIntent(R.id.noti_playButton, pendingIntent);

            Intent nextIntent = new Intent(this, MusicBroadcastReceiver.class);
            nextIntent.setAction(NEXT);
            PendingIntent pendingIntent1 = PendingIntent.getBroadcast(getBaseContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifView.setOnClickPendingIntent(R.id.noti_nextButton, pendingIntent1);

            if (_isPlaying) {
                notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_pause_black_24dp);
            } else {
                notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_play_arrow_black_24dp);
            }
            notification = builder.build();
            notification.contentView = notifView;
            manager.notify(NOTOFICATION_ID, notification);
        }

        @Override
        public void onAudioFocusChange(int i) {
            if (i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                //Lost audio focus
                pauseSong();
            } else if (i == AudioManager.AUDIOFOCUS_GAIN) {
                //Gained focus
                if (player != null && _isPlaying)
                    playSong();
            } else if (i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (player != null && player.isPlaying()) {
                    player.setVolume(0.1f, 0.1f);
                }
            }
        }

        public class MusicBinder extends Binder {
            public MediaPlayerService getService() {
                return MediaPlayerService.this;
            }
        }

        //binder class to return when this service is bound
        private final IBinder musicBind = new MusicBinder();

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return musicBind;
        }

        @Override
        public boolean onUnbind(Intent intent) {
            Log.d("LOG", "service unbind called");
            if (timer != null) {
                timer.cancel();
                timer.purge();
            }
            timer = null;
            player.stop();
            player.reset();
            player.release();
            playing = false;
            _isPlaying = false;
            stopForeground(true);
            _applicationRunning = false;
            return false;
        }

        @Override
        public void onDestroy() {
            Log.d("LOG", "service on destroy called");
            super.onDestroy();
        }

        private void loadAlbumArt(File file) {

            Bitmap bitmap = getAlbumArt(file);
            if (bitmap == null || bitmap.getByteCount() <= 0) {
                //album art not found from file, setting default art
                _albumArt.setImageResource(R.drawable.album_art);
            } else {
                _albumArt.setImageBitmap(bitmap);
            }
        }

        private Bitmap getAlbumArt(File file) {
            if (file == null)
                return null;
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(file.getAbsolutePath());
            byte[] bytes = mmr.getEmbeddedPicture();
            mmr.release();
            Bitmap bitmap = null;
            if (bytes != null) {
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            return bitmap;
        }

        /**
         * Prepare the player,
         * mainly prepare album art and song title. This
         * method should be called <b>only once</b>, at the application start time.
         *
         * @param file
         */
        public void prepareForPlayback(File file) {
            _songTitle.setText(file.getName());
            loadAlbumArt(file);
        }

        /**
         * start the playback
         */
        private void playSong() {
            //_play a song
            if (!onPause) {
                nowPlaying = null;
                System.gc();
                nowPlaying = MusicLibraryController.current();

                _songTitle.setText(nowPlaying.getName());
                loadAlbumArt(nowPlaying);

                player.reset();
                Uri uri = Uri.fromFile(nowPlaying);
                try {
                    player.setDataSource(getApplicationContext(), uri);
                } catch (Exception e) {
                    Log.e("MUSIC SERVICE", "Error setting data source", e);
                }
                player.prepareAsync();
            } else {
                player.seekTo(currentPlayerPosition);
                player.start();
                onPause = false;
            }
        }

        public void playSong(File music) {
            //_play a song
            if (!onPause) {
                nowPlaying = null;
                System.gc();
                nowPlaying = music;

                _songTitle.setText(nowPlaying.getName());
                loadAlbumArt(nowPlaying);
                if (player.isPlaying()) {
                    player.stop();
                }
                player.reset();
                Uri uri = Uri.fromFile(nowPlaying);
                try {
                    player.setDataSource(getApplicationContext(), uri);
                } catch (Exception e) {
                    Log.e("MUSIC SERVICE", "Error setting data source", e);
                }
                player.prepareAsync();
            } else {
                player.seekTo(currentPlayerPosition);
                player.start();
                onPause = false;
            }
        }

        public void playSong(File music, int position) {
            //_play a song
            currentPlayerPosition = position;
            onPause = false;
            playSong(music);
        }

        public void pauseSong() {
            if (player.isPlaying()) {
                currentPlayerPosition = player.getCurrentPosition();
                player.pause();
                onPause = true;
            }
        }

        public void playNextSong(File music) {
            currentPlayerPosition = 0;
            playSong(music);
        }

        public void playPrevSong(File music) {
            currentPlayerPosition = 0;
            playSong(music);
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            Log.d("service", "playback complete");
            playing = false;
            currentPlayerPosition = 0;
            playSong(MusicLibraryController.next());
            showNotification(nowPlaying);
        }

        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            return false;
        }

        private Timer timer;

        @Override
        public void onPrepared(final MediaPlayer mediaPlayer) {

            Log.d("service", "playback started");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mediaPlayer.start();
            mediaPlayer.seekTo(currentPlayerPosition);
            playing = true;     //the service is started and running, the player may be paused

            startForeground(NOTOFICATION_ID, notification);

            int dur = mediaPlayer.getDuration();
            int min = dur / 1000 / 60;
            int sec = dur - min * 60 * 1000;
            sec = (int) Math.ceil(sec / 1000);
            if (sec < 10)
                _totTime.setText(min + ":0" + sec);
            else
                _totTime.setText(min + ":" + sec);

            _seekBar.setMax(dur);

            /*AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    while (_applicationRunning && mediaPlayer!=null) {
                        if ( mediaPlayer.isPlaying() && _isPlaying) {
                            //updating the current time
                            _currentPos = mediaPlayer.getCurrentPosition();
                            //updating seekbar
                            if (!_touchingSeekBar) {
                                //Log.d("Seekbar",_currentPos+"");
                                _seekBar.setProgress(_currentPos);
                            }
                        }
                        try {
                            Log.d("THREAD","I am running");
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });*/
        }

    }


    /**
     * Song list fragment class
     */
    public static class SongListFragment extends Fragment {


        public SongListFragment() {
            // Required empty public constructor
        }

        private ArrayAdapter<String> arrayAdapter;
        ListView listView;
        ArrayList<String> songs;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View v = inflater.inflate(R.layout.fragment_song_list, container, false);

            initializeTabs(v);

            songs = new ArrayList<>();

            for (String s : MusicLibraryController.getMusicLibrary()) {
                songs.add(s.substring(s.lastIndexOf("/") + 1));
            }

            listView = (ListView) v.findViewById(R.id.listView);

            arrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.song_list_item, songs);

            listView.setAdapter(arrayAdapter);
            arrayAdapter.notifyDataSetChanged();

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    getFragmentManager().beginTransaction().setCustomAnimations(R.anim.frag_enter_from_right,R.anim.frag_exit_to_left,
                            R.anim.frag_enter_from_left,R.anim.frag_exit_to_right).remove(SongListFragment.this).commit();
                    getFragmentManager().popBackStack();

                    MainActivity.MediaPlayerService musicSrv = MainActivity.getMusicSrv();
                    musicSrv.playSong(MusicLibraryController.getFile(i), 0);
                    _play.setImageResource(R.drawable.ic_pause_black_24dp);
                    _isPlaying = true;
                    showNotification(musicSrv.nowPlaying);
                }
            });

            return v;
        }

        private void showNotification(final File nowPlaying) {
            notifView.setImageViewBitmap(R.id.noti_albumart, getAlbumArt(nowPlaying));
            notifView.setTextViewText(R.id.noti_title, getMediaTitle(nowPlaying));
            notifView.setTextViewText(R.id.noti_artist, getArtistName(nowPlaying));

            Intent playPauseIntent = new Intent(getContext(), MusicBroadcastReceiver.class);
            playPauseIntent.setAction(PLAY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifView.setOnClickPendingIntent(R.id.noti_playButton, pendingIntent);

            Intent nextIntent = new Intent(getContext(), MusicBroadcastReceiver.class);
            nextIntent.setAction(NEXT);
            PendingIntent pendingIntent1 = PendingIntent.getBroadcast(getContext(), 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notifView.setOnClickPendingIntent(R.id.noti_nextButton, pendingIntent1);

            if (_isPlaying) {
                notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_pause_black_24dp);
            } else {
                notifView.setImageViewResource(R.id.noti_playButton, R.drawable.ic_play_arrow_black_24dp);
            }
            notification = builder.build();
            notification.contentView = notifView;
            manager.notify(NOTOFICATION_ID, notification);
        }

        private void initializeTabs(View v) {
            final TabHost host = (TabHost) v.findViewById(R.id.tabHost);
            host.setup();

            final TabWidget tabWidget = host.getTabWidget();

            //Tab 1
            TabHost.TabSpec spec = host.newTabSpec("All songs");
            spec.setContent(R.id.tab1);
            spec.setIndicator("All songs");
            host.addTab(spec);

            //Tab 2
            spec = host.newTabSpec("Tab Two");
            spec.setContent(R.id.tab2);
            spec.setIndicator("Tab Two");
            host.addTab(spec);

            //Tab 3
            spec = host.newTabSpec("Tab Three");
            spec.setContent(R.id.tab3);
            spec.setIndicator("Tab Three");
            host.addTab(spec);

            for (int i = 0; i < host.getTabWidget().getChildCount(); i++) {
                TextView textView = (TextView) tabWidget.getChildAt(i).findViewById(android.R.id.title);
                if (i != 0)
                    textView.setTextColor(Color.parseColor("#a5a4a4"));
                else
                    textView.setTextColor(Color.parseColor("#FFFFFF"));
            }

            host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
                @Override
                public void onTabChanged(String s) {
                    int idx = host.getCurrentTab();
                    Log.d("TAB", "tab clicked = " + idx);
                    TextView textView = (TextView) tabWidget.getChildAt(idx).findViewById(android.R.id.title);
                    textView.setTextColor(Color.WHITE);
                    for (int i = 0; i < tabWidget.getChildCount(); i++) {
                        if (i == idx) continue;
                        textView = (TextView) tabWidget.getChildAt(i).findViewById(android.R.id.title);
                        textView.setTextColor(Color.parseColor("#a5a4a4"));
                    }
                }
            });
        }

    }

}
