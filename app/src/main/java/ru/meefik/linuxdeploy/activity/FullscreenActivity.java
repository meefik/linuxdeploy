package ru.meefik.linuxdeploy.activity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;

public class FullscreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_fullscreen);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Screen lock
        if (PrefStore.isScreenLock(this))
            this.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else this.getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set screen orientation (freeze)
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }

    }
}
