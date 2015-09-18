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
		TextView atv = (TextView) findViewById(R.id.AboutTextView);
		atv.setMovementMethod(LinkMovementMethod.getInstance());
		TextView vtv = (TextView) findViewById(R.id.VersionView);
		vtv.setText(getString(R.string.app_version, PrefStore.VERSION));
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
		String url = null;
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.menu_donate:
			url = "http://meefik.github.io/donate";
			break;
		case R.id.menu_forum:
			url = "http://4pda.ru/forum/index.php?showtopic=378043";
			break;
		case R.id.menu_doc:
			url = "https://github.com/meefik/linuxdeploy/wiki";
			break;
		case R.id.menu_issues:
			url = "https://github.com/meefik/linuxdeploy/issues";
			break;
		case R.id.menu_source:
			url = "https://github.com/meefik/linuxdeploy";
			break;
		}
		if (url != null) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
		}
		return false;
	}

}
