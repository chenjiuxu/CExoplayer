package chenjiuxu.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

/**
 * Created by C.jiuxu on 2017/5/2.
 */
public class CExoPlayerView extends FrameLayout {

    private final AspectRatioFrameLayout contentFrame;
    private final ImageView artworkView;//封面
    private final View shutterView;//快门
    private ComponentListener componentListener;
    private final Context context;
    private SurfaceView surfaceView;
    private CPlayControlView controlView;
    private SimpleExoPlayer player;
    private DefaultBandwidthMeter bandwidthMeter;
    private long position = 0;
    private boolean isPlay;//是否播放
    private boolean isEnd;
    private ArrayList<String> videoList;
    private int windowIndex = 0;//当前播放第几个视频

    public CExoPlayerView(Context context) {
        this(context, null);
    }

    public CExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        int resource = R.layout.c_exo_player_view;
        int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
        LayoutInflater.from(context).inflate(resource, this);//为页面加载布局
        componentListener = new ComponentListener();
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);//焦点的能力 ChildView先处理

        contentFrame = (AspectRatioFrameLayout) findViewById(R.id.c_exo_player_view_arfl);//视频容器
        if (contentFrame != null) setResizeModeRaw(contentFrame, resizeMode);//视频比例
        if (contentFrame != null) {
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            surfaceView = new SurfaceView(context);//添加surfaceView 绘制视频
            surfaceView.setLayoutParams(params);
            contentFrame.addView(surfaceView, 0);
        }

        shutterView = findViewById(R.id.c_exo_player_view_shutter);
        artworkView = (ImageView) findViewById(R.id.c_exo_player_view_img);//视频封面

        View controllerPlaceholder = findViewById(R.id.c_exo_player_view_control);//控制组件替换
        if (controllerPlaceholder != null) {
            controlView = new CPlayControlView(context, this, attrs);
            controlView.setLayoutParams(controllerPlaceholder.getLayoutParams());
            ViewGroup parent = ((ViewGroup) controllerPlaceholder.getParent());
            int controllerIndex = parent.indexOfChild(controllerPlaceholder);
            parent.removeView(controllerPlaceholder);
            parent.addView(controlView, controllerIndex);
        }
        videoList = new ArrayList<>();
    }

    /**
     * 视频比例模式
     */
    private void setResizeModeRaw(AspectRatioFrameLayout aspectRatioFrame, int resizeMode) {
        aspectRatioFrame.setResizeMode(resizeMode);
    }


    public void prepare() {
        SimpleExoPlayer simpleExoPlayer = createPlayer();
        setPlayer(simpleExoPlayer);
    }

    /**
     * 单个开始播放
     */
    public void startPlay(String url) {
        String videoUrl = null;
        if (videoList != null && videoList.size() == 1) videoUrl = videoList.get(0);
        if (TextUtils.equals(url, videoUrl)) {//相同 重新播放 或者提示
            controlView.anewPaly();
        } else {//播放链接不同时
            shutterView.setVisibility(VISIBLE);
            player.stop();
            position = 0;
            windowIndex = 0;
            videoList.clear();
            videoList.add(url);
            play(position);


        }
    }
    /**
     * 多个视频播放
     */
    public void startPlay(String[] urls) {
        shutterView.setVisibility(VISIBLE);
        player.stop();
        position = 0;
        windowIndex = 0;
        videoList.clear();
        for (String url : urls) videoList.add(url);
        play(position);
    }

    /**
     * 数据填充
     */
    private void play(long position) {
        if (videoList.isEmpty()) throw new NullPointerException("视频uri为空");
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "kandaola"), bandwidthMeter);//媒体数据加载产生的数据源实例。
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();//媒体数据生成解析器实例。
        MediaSource[] mediaSourceList = new MediaSource[videoList.size()];
        for (int i = 0; i < videoList.size(); i++) {
            MediaSource videoSource = new ExtractorMediaSource(Uri.parse(videoList.get(i)), dataSourceFactory, extractorsFactory, null, null);//这是MediaSource代表媒体播放。
            mediaSourceList[i] = videoSource;
        }
        //   LoopingMediaSource loopingSource = new LoopingMediaSource(videoSource);//循环播放
        ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource(mediaSourceList);//播放视频序列
        player.prepare(concatenatedSource);//准备有源代码的玩家
        player.seekTo(windowIndex, position);
        player.setPlayWhenReady(true);
        isEnd = false;
    }

    /**
     * 播放速度
     */
    private void setPlaySpeed(float speed) {
        PlaybackParameters playbackParameters = new PlaybackParameters(speed, speed);//变速播放
        player.setPlaybackParameters(playbackParameters);
    }


    /**
     * 创建player
     */
    private SimpleExoPlayer createPlayer() {
        bandwidthMeter = new DefaultBandwidthMeter();//估计听数据传输带宽。带宽估计计算使用
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);//网络状态监听
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        return player;
    }


    /**
     * 设置SimpleExoPlayer
     */
    private void setPlayer(SimpleExoPlayer player) {
        if (this.player != null) {
            this.player.clearVideoListener(componentListener);
            this.player.clearVideoSurfaceView(surfaceView);
        }
        this.player = player;
        controlView.setPlayer(player);
        if (shutterView != null) shutterView.setVisibility(VISIBLE);//显示遮盖
        if (player != null) {
            player.setVideoSurfaceView(surfaceView);
            player.setVideoListener(componentListener);
        }
    }

    /**
     * 重新开始播放
     */
    public void restartPlay() {
        play(position);
    }

    /**
     * 界面重新开始
     */
    protected void restart() {
        if (!isPlay) return;
        if (isEnd) return;
        restartPlay();
    }

    /**
     * 界面销毁
     */
    protected void destroy() {//释放资源
        this.player.clearVideoListener(componentListener);
        componentListener = null;
        player.release();
        player = null;
    }

    /**
     * 暂停
     */
    protected void pause() {
        saveState();
        player.stop();//停止播放
    }

    /**
     * 保存播放器状态
     */
    private void saveState() {
        int state = player.getPlaybackState();
        windowIndex = player.getCurrentPeriodIndex();
        switch (state) {
            case ExoPlayer.STATE_READY://可以播放状态
                position = player.getCurrentPosition();//记录播放时间
                isPlay = player.getPlayWhenReady();
                break;
            case ExoPlayer.STATE_ENDED://播放完成
                position = 0;//记录播放时间
                windowIndex = 0;
                isEnd = true;
                break;
        }
    }

    /**
     * 状态监听器
     */
    private class ComponentListener implements SimpleExoPlayer.VideoListener {
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {//视频的大小发生变化
            if (contentFrame != null) {
                float aspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
                contentFrame.setAspectRatio(aspectRatio);
            }
        }

        @Override
        public void onRenderedFirstFrame() {//开始绘制时
            if (shutterView != null) {
                shutterView.setVisibility(INVISIBLE);
            }
            isEnd = false;
        }
    }
}
