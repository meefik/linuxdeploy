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

public class ComponentsActivity extends SherlockActivity {
	
	private ListView listView;
	private ArrayList<String> listItems = new ArrayList<String>();
	private ArrayAdapter<String> adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_components);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		listView = (ListView) findViewById(R.id.componentsView);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, listItems);
		listView.setAdapter(adapter);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.updateLocale(getApplicationContext());
		getSupportMenuInflater().inflate(R.menu.activity_components, menu);
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
		
		String titleMsg = this.getString(R.string.title_activity_components)
				+ ": " + PrefStore.getCurrentProfile(getApplicationContext());
		this.setTitle(titleMsg);
		
		PrefStore.get(getApplicationContext());
		
		String[] array = null;
		if (PrefStore.DISTRIB.equals("debian")) {
			array = getResources().getStringArray(R.array.debian_components_values);
		}
		if (PrefStore.DISTRIB.equals("ubuntu")) {
			array = getResources().getStringArray(R.array.ubuntu_components_values);
		}
		if (PrefStore.DISTRIB.equals("archlinux")) {
			array = getResources().getStringArray(R.array.archlinux_components_values);
		}
		if (PrefStore.DISTRIB.equals("fedora")) {
			array = getResources().getStringArray(R.array.fedora_components_values);
		}
		if (PrefStore.DISTRIB.equals("kali")) {
			array = getResources().getStringArray(R.array.kali_components_values);
		}
		if (PrefStore.DISTRIB.equals("gentoo")) {
			array = getResources().getStringArray(R.array.gentoo_components_values);
		}
		listItems.addAll(Arrays.asList(array));
		
		List<String> list = PrefStore.getComponentsList(getApplicationContext());
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
		PrefStore.setComponentsList(getApplicationContext(), list);
	}

}
