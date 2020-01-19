package ru.meefik.linuxdeploy.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.fragment.PropertiesFragment;

public class PropertiesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_preference);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new PropertiesFragment())
                .commit();

        // Restore from conf file if open from main activity
        if (getIntent().getBooleanExtra("restore", false)) {
            PrefStore.restoreProperties(this);
        }
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    protected void onResume() {
        super.onResume();

        String titleMsg = getString(R.string.title_activity_properties)
                + ": " + PrefStore.getProfileName(this);
        setTitle(titleMsg);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Update configuration file
        PrefStore.dumpProperties(this);
    }
}
