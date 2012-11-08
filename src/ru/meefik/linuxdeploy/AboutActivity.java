package ru.meefik.linuxdeploy;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_about);
		TextView tv = (TextView) findViewById(R.id.AboutTextView);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void onResume() {
		super.onResume();
		this.setTitle(R.string.title_activity_about);
	}

}
