package ru.meefik.linuxdeploy;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.app.Activity;

public class AboutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AppPrefs.updateLocale(getBaseContext());
        
        setContentView(R.layout.activity_about);
        
        TextView tv = (TextView) findViewById(R.id.AboutTextView);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
