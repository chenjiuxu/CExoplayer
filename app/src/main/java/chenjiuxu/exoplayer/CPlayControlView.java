package chenjiuxu.exoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

/**
 * Created by 15705 on 2017/5/2.
 */

public class CPlayControlView extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private final View load_view;//加载提示
    private final View control_view;//控制条
    private final TextView time_current_tv;//当前时间
    private final TextView time_tv;//总时长
    private final SeekBar seekBar;
    private final ComponentListener componentListener;//监听器
    private final View play_bt;//大播键
    private final ImageView play;//小播放键
    private final ImageView full_screen;//全屏
    private final Activity context;
    private CExoPlayerView cExoPlayerView;//播放组件
    private SimpleExoPlayer player;
    private int showTime = 4000;
    private int updateTime = 1000;
    //更新
    private final Runnable updateAction = new Runnable() {
        @Override
        public void run() {
            updateView();
        }
    };
    //隐藏
    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hideControlStrip();
        }
    };
    private Point point;

    public CPlayControlView(Context context) {
        this(context, null, null);
    }

    public CPlayControlView(Context context, CExoPlayerView cExoPlayerView, AttributeSet attrs) {
        this(context, attrs, 0);
        this.cExoPlayerView = cExoPlayerView;
    }

    public CPlayControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = (Activity) context;
        int controllerLayoutId = R.layout.c_exo_player_control_view;
        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        load_view = findViewById(R.id.c_exo_player_control_view_load);
        control_view = findViewById(R.id.c_exo_player_control_view_control);
        play_bt = findViewById(R.id.c_exo_player_control_view_play_bt);
        play = (ImageView) findViewById(R.id.c_exo_player_control_view_play);
        full_screen = (ImageView) findViewById(R.id.c_exo_player_control_view_full_screen);
        time_current_tv = (TextView) findViewById(R.id.c_exo_player_control_view_time_current);
        seekBar = (SeekBar) findViewById(R.id.c_exo_player_control_view_progress);
        seekBar.setOnSeekBarChangeListener(this);
        time_tv = (TextView) findViewById(R.id.c_exo_player_control_view_time);
        componentListener = new ComponentListener();
        setOnClickListener(this);
        play_bt.setOnClickListener(this);
        play.setOnClickListener(this);
        full_screen.setOnClickListener(this);
    }

    public void setPlayer(SimpleExoPlayer p) {
        if (player == p) return;
        if (player != null) this.player.removeListener(componentListener);
        player = p;
        if (player != null) player.addListener(componentListener);
    }

    @Override
    public void onClick(View v) {//点击监听
        if (v == this) {
            showControlStrip();
        } else {
            switch (v.getId()) {
                case R.id.c_exo_player_control_view_play://播放
                case R.id.c_exo_player_control_view_play_bt://播放
                    playAnPause();
                    break;
                case R.id.c_exo_player_control_view_full_screen://全屏
                    screen();
                    break;
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(updateAction);
        removeCallbacks(hideAction);
    }

    /**
     * 显示控制条
     */
    private void showControlStrip() {
        if (control_view.getVisibility() != VISIBLE) {
            replyInit();
            control_view.setVisibility(VISIBLE);
            postDelayed(hideAction, showTime);
            postOnAnimation(updateAction);
        } else {
            hideControlStrip();
        }
    }

    /**
     * 隐藏控制条
     */
    private void hideControlStrip() {
        control_view.setVisibility(INVISIBLE);
        removeCallbacks(updateAction);
        removeCallbacks(hideAction);
    }

    /**
     * 更新界面播放参数
     */
    private void updateView() {
        int durrentPosition = (int) player.getCurrentPosition();
        int current = durrentPosition / 1000;
        int fen = current / 60;
        int miao = current % 60;
        String fs = (fen >= 10) ? fen + "" : "0" + fen;
        String ms = (miao >= 10) ? miao + "" : "0" + miao;
        String time = new StringBuffer(fs).append(":").append(ms).toString();
        seekBar.setProgress(durrentPosition);
        time_current_tv.setText(time);
        removeCallbacks(updateAction);
        if (player.getPlayWhenReady()) postDelayed(updateAction, updateTime);
    }

    /**
     * 播放或者暂停
     **/
    private void playAnPause() {
        int state = player.getPlaybackState();
        switch (state) {
            case ExoPlayer.STATE_IDLE://没有数据源 stop
                cExoPlayerView.restartPlay();
                updateView(true);
                break;
            case ExoPlayer.STATE_BUFFERING://缓冲
                break;
            case ExoPlayer.STATE_READY://可以播放状态
                readyPlayAnPause();
                break;
            case ExoPlayer.STATE_ENDED://播放完成
                endedPlayAnPause();
                break;
        }
    }

    /**
     * 播放完成 暂停 播放
     */
    private void endedPlayAnPause() {
        player.seekTo(0, 0);
        player.setPlayWhenReady(true);
        updateView(true);
    }

    /**
     * 可以播放状态 暂停 播放
     */
    private void readyPlayAnPause() {
        boolean isPlay = !player.getPlayWhenReady();
        player.setPlayWhenReady(isPlay);
        updateView(isPlay);
    }

    /**
     * 播放状态改变界面改变
     */
    private void updateView(Boolean isPlay) {
        if (isPlay) {//播放
            play_bt.setVisibility(INVISIBLE);
            play.setImageResource(R.drawable.exo_controls_pause);
        } else {//暂停
            play_bt.setVisibility(VISIBLE);
            play.setImageResource(R.drawable.exo_controls_play);
        }

    }

    /**
     * 全屏
     */
    private void screen() {
        if (context.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {//是横屏
            setViseoWH();
            context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setViseoFullScreen();
            context.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    /**
     * 设置视频播放框大小
     */
    private void setViseoWH() {
        ViewGroup.LayoutParams layoutParams = cExoPlayerView.getLayoutParams();
        if (point == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            point = new Point();
            wm.getDefaultDisplay().getSize(point);
        }
        int w = point.x;
        layoutParams.height = w / 16 * 9;
        layoutParams.width = w;
        cExoPlayerView.setLayoutParams(layoutParams);
    }

    /**
     * 全屏
     */
    private void setViseoFullScreen() {
        ViewGroup.LayoutParams layoutParams = cExoPlayerView.getLayoutParams();
        if (point == null) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            point = new Point();
            wm.getDefaultDisplay().getSize(point);
        }
        layoutParams.height = point.x;
        layoutParams.width = point.y;
        cExoPlayerView.setLayoutParams(layoutParams);
    }

    /**
     * 重新播放
     */
    public void anewPaly() {
        if (player.getPlaybackState() == ExoPlayer.STATE_ENDED) {//视频结束标记  重新播放
            playAnPause();
        } else {
            Toast.makeText(context, "视频正在播放", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 播放状态
     */
    private void playState() {
        load_view.setVisibility(INVISIBLE);//隐藏加载
        play_bt.setVisibility(INVISIBLE);//隐藏大播放键
    }

    /**
     * 播放结束
     */
    private void playEndState() {
        load_view.setVisibility(INVISIBLE);
        play_bt.setVisibility(VISIBLE);//隐藏大播放键
        play.setImageResource(R.drawable.exo_controls_play);
    }

    /**
     * 控制组件回复初始化
     */
    public void replyInit() {
        play.setImageResource(R.drawable.exo_controls_pause);
        int duration = (int) player.getDuration();
        seekBar.setMax(duration);
        seekBar.setProgress(0);
        int second = duration / 1000;
        time_tv.setText(second / 60 + ":" + second % 60);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        player.seekTo(seekBar.getProgress());
    }

    /**
     * 监听器
     */
    private class ComponentListener implements ExoPlayer.EventListener {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady) {
                switch (playbackState) {
                    case ExoPlayer.STATE_IDLE://无数据状态
                        break;
                    case ExoPlayer.STATE_BUFFERING://数据缓存
                        load_view.setVisibility(VISIBLE);
                        break;
                    case ExoPlayer.STATE_READY://播放
                        playState();
                        break;
                    case ExoPlayer.STATE_ENDED://播放结束
                        playEndState();
                        break;
                }
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity() {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }
    }


}
