package ru.meefik.linuxdeploy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AboutActivity extends SherlockActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_about);
		TextView tv = (TextView) findViewById(R.id.AboutTextView);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public void onResume() {
		super.onResume();
		this.setTitle(R.string.title_activity_about);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.updateLocale(getApplicationContext());
		getSupportMenuInflater().inflate(R.menu.activity_about, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.menu_donate:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://meefik.github.io/donate"));
			startActivity(browserIntent);
			break;
		}
		return false;
	}

}
