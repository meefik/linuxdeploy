package ru.meefik.linuxdeploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PackagesActivity extends SherlockActivity {
	
	private ListView listView;
	private ArrayList<String> listItems = new ArrayList<String>();
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_packages);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		listView = (ListView) findViewById(R.id.packagesView);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, listItems);
		listView.setAdapter(adapter);
		String[] array = getResources().getStringArray(R.array.debian_packages_values);
		listItems.addAll(Arrays.asList(array));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.updateLocale(getApplicationContext());
		getSupportMenuInflater().inflate(R.menu.activity_packages, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.menu_markall:
			for ( int i=0; i< listView.getCount(); i++ ) {
				listView.setItemChecked(i, true);
			}			
			break;
		case R.id.menu_clearall:
			for ( int i=0; i< listView.getCount(); i++ ) {
				listView.setItemChecked(i, false);
			}
			break;
		}
		return false;		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		String titleMsg = this.getString(R.string.title_activity_packages)
				+ ": " + PrefStore.getCurrentProfile(getApplicationContext());
		this.setTitle(titleMsg);
		
		List<String> list = PrefStore.getPackagesList(getApplicationContext());
		for (int i = 0; i < listItems.size(); i++) {
			for (String pkg: list) {
				if (listItems.get(i).equals(pkg)) {
					listView.setItemChecked(i,true);
					break;
				}
			}
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < listItems.size(); i++) {
			if (listView.isItemChecked(i)) {
				list.add(listItems.get(i));
			}
		}
		PrefStore.setPackagesList(getApplicationContext(), list);
	}

}
