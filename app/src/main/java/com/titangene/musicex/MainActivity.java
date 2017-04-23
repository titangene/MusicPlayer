package com.titangene.musicex;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView txtMnsicOrder, txtMusic, txtProgressCurrent, txtProgressLength;
    private SeekBar song_progress;
    private ImageButton imgShuffle, imgPrev, imgPlayPause, imgNext, imgRepeat, imgEnd;
    private ListView lstMusic;

    private MediaPlayer mediaPlayer;
    private final String SONGPATH = Environment.getExternalStorageDirectory().getPath() + "/";
    private String path;
    // 歌曲名稱
    private String[] songName = new String[]
            {"Pyramids", "Floaters", "Growler", "Serenity", "Code_Blue",
                    "Cosmos", "Eviction", "Noel", "Laserstart", "Supreme"};
    // 歌曲檔案
    private String[] songFile = new String[]
            {"Pyramids.mp3", "Floaters.mp3", "Growler.mp3", "Serenity.mp3", "Code_Blue.mp3",
                    "Cosmos.mp3", "Eviction.mp3", "Noel.mp3", "Laserstart.mp3", "Supreme.mp3"};
    private int cListItem = 0;  // 目前播放歌曲

    private boolean isMediaPlayerPrepare = false;

    // 儲存隨機歌曲順序的清單
    private List<Integer> songListShuffleOrder = new ArrayList<Integer>();
    // 儲存隨機歌曲順序的清單 (從 1 開始編號)
    private List<Integer> songListShuffleOrderTemp = new ArrayList<Integer>();
    // 原始歌曲順序的清單
    private List<Integer> songListOriginalOrder = new ArrayList<Integer>();

    private enum PlayState {none, play, pause}      // 播放狀態
    private PlayState playState = PlayState.none;

    private enum RepeatState {none, repeat, repeat_one}     //　循環播放狀態
    private RepeatState repeatState = RepeatState.none;

    private boolean isShuffle = false;  // 是否隨機撥放

    private int progressCurrent;    // 目前播放時間
    private int progressTotal;      // 總播放時間

    // 更新目前播放時間
    private Message msg;
    private int time;
    private int mtime;
    private int stime;
    private String mtimeStr;
    private String stimeStr;

    private Timer mTimer = new Timer();
    private boolean isStartPlayMusic;    // 是否讓 mTimerTask 開始執行
    private boolean isFirstUseTimer = true;     // 第一次啟用產生 Timer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtMnsicOrder = (TextView)findViewById(R.id.txtMnsicOrder);
        txtMusic = (TextView)findViewById(R.id.txtMusic);
        txtProgressCurrent = (TextView)findViewById(R.id.txtProgressCurrent);
        txtProgressLength = (TextView)findViewById(R.id.txtProgressLength);
        song_progress = (SeekBar)findViewById(R.id.song_progress);
        imgShuffle = (ImageButton)findViewById(R.id.imgShuffle);
        imgPrev = (ImageButton)findViewById(R.id.imgPrev);
        imgPlayPause = (ImageButton)findViewById(R.id.imgPlayPause);
        imgNext = (ImageButton)findViewById(R.id.imgNext);
        imgRepeat = (ImageButton)findViewById(R.id.imgRepeat);
        imgEnd = (ImageButton)findViewById(R.id.imgEnd);
        lstMusic = (ListView)findViewById(R.id.lstMusic);

        song_progress.setOnSeekBarChangeListener(progressListener);
        imgShuffle.setOnClickListener(btnListener);
        imgPrev.setOnClickListener(btnListener);
        imgPlayPause.setOnClickListener(btnListener);
        imgNext.setOnClickListener(btnListener);
        imgRepeat.setOnClickListener(btnListener);
        imgEnd.setOnClickListener(btnListener);
        lstMusic.setOnItemClickListener(lstListener);

        for (int i = 0; i < songName.length; i++) {
            // 初始化 隨機播放、原始播放 順序清單
            songListShuffleOrder.add(new Integer(i));
            songListOriginalOrder.add(new Integer(i + 1));
            // 初始化 隨機撥放 順序清單 (從 1 開始編號)
            songListShuffleOrderTemp.add(new Integer(i + 1));
            // 在歌名前加上順序編號
            songName[i] = (i + 1) + ". " + songName[i];
        }

        txtMnsicOrder.setText("原始順序：" + songListOriginalOrder);
        // 預設未啟用 imgShuffle, imgRepeat，設定 icon 透明度
        imgShuffle.setImageAlpha(80);
        imgRepeat.setImageAlpha(80);

        mediaPlayer = new MediaPlayer();

        // TODO 改
        // 做好播放準備
        try {
            PreparePlay("");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失敗", Toast.LENGTH_LONG).show();
        }

        // Timer.schedule(TimerTask, 延遲毫秒, 每隔幾毫秒重複執行)
        // 更新 bar, 目前播放時間文字
        mTimer.schedule(mTimerTask, 0, 300);

        ArrayAdapter<String> adaSong =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, songName);

        //lstMusic.setSelector(R.color.listSelect);

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            lstMusic.setAdapter(adaSong);
        }
    }

    // 詢問是否允許權限
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {     // 按允許按鈕
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("取得權限")
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage("已取得權限，按確定按鈕結束應用程式後重新啟動")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();   // 結束應用程式
                        }
                    }).show();
            } else {
                Toast.makeText(this, "未取得權限！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ImageButton.OnClickListener btnListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.imgShuffle:   // 隨機撥放
                    shuffleSong();
                    break;

                case R.id.imgPrev:      // 上一首
                    prevSong();
                    break;

                case R.id.imgPlayPause: // 播放 / 暫停
                    PlayPauseSong();
                    break;

                case R.id.imgNext:      // 下一首
                    nextSong();
                    break;

                case R.id.imgRepeat:    // 循環播放
                    repeatSong();
                    break;

                case R.id.imgEnd:       // 結束
                    finish();
                    break;
            }
        }
    };

    private ListView.OnItemClickListener lstListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int posistion, long id) {
            cListItem = posistion;      // 取得點選位置
            // 點歌單時改成未準備狀態
            isMediaPlayerPrepare = false;
            playSong_checkShuffle();    // 播放
        }
    };

    private SeekBar.OnSeekBarChangeListener progressListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) { }

        // 拖移進度條時，TimerTask 暫停執行，不然在你沒放開手指前 bar 會一直亂跳
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isStartPlayMusic = false;
        }

        // 放開進度條時，TimerTask 執行，歌曲會因為你拖移進度條放開後改變播放位置
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isStartPlayMusic = true;
            progressCurrent = song_progress.getProgress();
            mediaPlayer.seekTo(progressCurrent);
        }
    };

    // update song_progress，隨著歌曲播放而改變bar位置
    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            if(mediaPlayer != null && isStartPlayMusic) {
                progressCurrent = mediaPlayer.getCurrentPosition();
                song_progress.setProgress(progressCurrent);

                msg = handler.obtainMessage();
                handler.sendMessage(msg);
            }
        }
    };

    // 更新目前播放時間文字
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            convertSongTime_SetTxt(txtProgressCurrent, progressCurrent);
        }
    };

    // 更新播放時間文字 (目前播放時間 or 總播放時間)
    private void convertSongTime_SetTxt(TextView txtProgress, int Time) {
        time = Time / 1000;
        mtime = time / 60;
        stime = time % 60;
        // 2位數補一個 0：String.format("%02d", mtime)： 2 --> 02
        mtimeStr = String.format("%02d", mtime);
        stimeStr = String.format("%02d", stime);
        txtProgress.setText(mtimeStr + ":" + stimeStr);
    }

    // 做好播放準備
    private void PreparePlay(String path) throws IOException {
        if (path == "")
            path = isShuffle ? SONGPATH + songFile[songListShuffleOrder.get(cListItem)] :
                    SONGPATH + songFile[cListItem];

        mediaPlayer.reset();                // 重製 MediaPlayer
        mediaPlayer.setDataSource(path);    // 播放歌曲路徑
        mediaPlayer.prepare();              // 多媒體準備播放

        progressTotal = mediaPlayer.getDuration();  // 取得總播放時間
        song_progress.setMax(progressTotal);
        song_progress.setProgress(progressCurrent);
        mediaPlayer.seekTo(progressCurrent);
        // 更新總播放時間文字
        convertSongTime_SetTxt(txtProgressLength, progressTotal);
        updateSongName("checkRepeat");    // 更新歌名

        isStartPlayMusic = true;
        isMediaPlayerPrepare = true;
    }

    private void PlayPauseSong() {
        switch (playState) {
            // 未播放狀態 (第一次開啟程式 or 循環播放播完所有歌曲，結束播放) 按按鈕 -> 從第一首開始播放
            case none:
                playSong_checkShuffle();
                playState = PlayState.play;
                imgPlayPause.setImageResource(R.drawable.ic_pause);
                break;

            // 播放狀態按按鈕 -> 暫停播放
            case play:
                mediaPlayer.pause();
                playState = PlayState.pause;
                imgPlayPause.setImageResource(R.drawable.ic_play_arrow);
                break;

            // 暫停狀態按按鈕 -> 繼續播放
            case pause:
                mediaPlayer.start();    // 開始播放
                playState = PlayState.play;
                imgPlayPause.setImageResource(R.drawable.ic_pause);
                break;
        }
    }

    private void playSong(String path) {
        try {
            if (!isMediaPlayerPrepare) {
                PreparePlay(path);
            }

            // TODO 改
//            if (isFirstUseTimer) {  // 第一次使用 Timer
//                // Timer.schedule(TimerTask, 延遲毫秒, 每隔幾毫秒重複執行)
//                mTimer.schedule(mTimerTask, 0, 300);
//                isFirstUseTimer = false;
//            }

            // TODO 永遠從開始播放
            mediaPlayer.start();    // 開始播放

            playState = PlayState.play;       // 改變播放狀態
            imgPlayPause.setImageResource(R.drawable.ic_pause);

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    // 播放完後暫停TimerTask
                    isStartPlayMusic = false;
                    // 播放完後改成未準備狀態
                    isMediaPlayerPrepare = false;
                    // 檢查循環播放狀態
                    playFinish_checkRepeat();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失敗", Toast.LENGTH_LONG).show();
        }
    }

    // 檢查隨機撥放狀態，使用 隨機 or 原始 播放順序
    private void playSong_checkShuffle() {
        if (isShuffle)
            playSong(SONGPATH + songFile[songListShuffleOrder.get(cListItem)]);
        else
            playSong(SONGPATH + songFile[cListItem]);
    }

    private void nextSong() {
        cListItem++;
        if (cListItem >= lstMusic.getCount())   // 若到最後就移到第一首
            cListItem = 0;

        progressCurrent = 0;
        isMediaPlayerPrepare = false;

        // 未播放時按下一首，不會馬上播放，只會切換到下一首等待播放
        if(playState == PlayState.play) {
            playSong_checkShuffle();
        } else {
            // 做好播放準備
            try {
                PreparePlay("");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "播放失敗", Toast.LENGTH_LONG).show();
            }
            playState = PlayState.none;
            imgPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    private void prevSong() {
        cListItem--;
        if (cListItem < 0)
            cListItem = lstMusic.getCount() - 1;    // 若到第一首就移到最後

        progressCurrent = 0;
        isMediaPlayerPrepare = false;

        if(playState == PlayState.play) {
            playSong_checkShuffle();
        } else {
            // 做好播放準備
            try {
                PreparePlay("");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "播放失敗", Toast.LENGTH_LONG).show();
            }
            playState = PlayState.none;
            imgPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    private void shuffleSong() {
        isShuffle = !isShuffle;
        if (isShuffle) {
            imgShuffle.setImageAlpha(255);
            // 洗牌，特色：可以 下一首或上一首 本次的隨機順序
            Collections.shuffle(songListShuffleOrder);

            for (int i = 0; i < songListShuffleOrder.size(); i++) {
                // 設定 隨機播放 順序清單 (從 1 開始編號)
                songListShuffleOrderTemp.set(i, songListShuffleOrder.get(i) + 1);
                // TODO 從原始順序 -> 隨機順序 的 上一首 or 下一首 錯亂
                if (songListShuffleOrder.get(i) == cListItem)
                    cListItem = i;
            }

            txtMnsicOrder.setText("隨機順序：" + songListShuffleOrderTemp);

        } else {
            imgShuffle.setImageAlpha(80);   // 改變 btn icon 透明度
            // 隨機到某首歌後解除隨機播放時，如按 下一首或上一首 都會正確切換
            // EX：隨機到第 5 首，但在隨機清單中是第 8 首，如果此時解除隨機播放，
            // 且按下一首或上一首時，可以切換至 4 or 6 首，而不是 7 or 9 首
            cListItem = songListShuffleOrder.get(cListItem);
            txtMnsicOrder.setText("原始順序：" + songListOriginalOrder);
        }
    }

    private void repeatSong() {
        switch (repeatState) {
            case none:
                repeatState = RepeatState.repeat;
                imgRepeat.setImageAlpha(255);
                imgRepeat.setImageResource(R.drawable.ic_repeat);
                updateSongName(" (循環播放)");
                break;

            case repeat:
                repeatState = RepeatState.repeat_one;
                imgRepeat.setImageResource(R.drawable.ic_repeat_one);
                updateSongName(" (單曲播放)");
                break;

            case repeat_one:
                repeatState = RepeatState.none;
                imgRepeat.setImageAlpha(80);    // 改變 btn icon 透明度
                imgRepeat.setImageResource(R.drawable.ic_repeat);
                updateSongName("");
                break;
        }
    }

    // 檢查循環播放狀態
    private void playFinish_checkRepeat() {
        switch (repeatState) {
            case none:
                // 如果播完最後一首歌
                if (cListItem >= songFile.length - 1) {
                    mediaPlayer.stop();
                    playState = PlayState.none;
                    imgPlayPause.setImageResource(R.drawable.ic_play_arrow);
                    cListItem = 0;
                    txtMusic.setText("播完所有歌曲，結束播放");
                } else
                    nextSong();
                break;

            case repeat:
                nextSong();     // 播放完後播下一首
                break;

            case repeat_one:
                playSong_checkShuffle();
                break;
        }
    }

    // 更新歌名
    private void updateSongName(String repeatStr) {
        // 檢查循環播放狀態
        if (repeatStr == "checkRepeat") {
            switch (repeatState) {
                case none:
                    repeatStr = "";
                    break;

                case repeat:
                    repeatStr = " (循環播放)";
                    break;

                case repeat_one:
                    repeatStr = " (單曲播放)";
                    break;
            }
        }

        if (isShuffle) {
            txtMusic.setText("歌名：" + songName[songListShuffleOrder.get(cListItem)] + repeatStr);
        } else {
            txtMusic.setText("歌名：" + songName[cListItem] + repeatStr);
        }
    }

    // mediaplayer 生命週期與 activity 生命週期無關，所以關閉 activity 也不會關閉音樂
    protected void onDestroy() {
        if(mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();

        mTimer.cancel();
        mediaPlayer.release();    // 釋放所有 MediaPlayer 的資源，呼叫後物件將完全失效
        super.onDestroy();
    }
}