package chenjiuxu.exoplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CExoPlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = (CExoPlayerView) findViewById(R.id.activity_main_cepv);
        findViewById(R.id.activity_main_bt).setOnClickListener(this);
        playerView.prepare();
        playerView.startPlay(new String[]{"http://www.kansight.com/videos/1454594246897-SHANGYUAN.mp4", "http://www.kansight.com/videos/14703117302424.mp4"});
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        playerView.restart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerView.destroy();
    }

    @Override
    public void onClick(View v) {
        playerView.startPlay("http://www.kansight.com/videos/1454594246897-SHANGYUAN.mp4");
    }
}
