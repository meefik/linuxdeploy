package ru.meefik.linuxdeploy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_about);
        TextView atv = (TextView) findViewById(R.id.aboutTextView);
        atv.setMovementMethod(LinkMovementMethod.getInstance());
        TextView vtv = (TextView) findViewById(R.id.versionView);
        vtv.setText(getString(R.string.app_version, PrefStore.getVersion(this)));
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.title_activity_about);
    }

}
