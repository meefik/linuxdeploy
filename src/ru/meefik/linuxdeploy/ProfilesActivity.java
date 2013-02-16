package ru.meefik.linuxdeploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ProfilesActivity extends SherlockActivity implements OnTouchListener {

	private ListView profilesList;
	private ArrayList<Profile<String, String>> listItems = new ArrayList<Profile<String, String>>();
	private ArrayAdapter<Profile<String, String>> adapter;
	private GestureDetector gd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		PrefStore.updateTheme(this);
		super.onCreate(savedInstanceState);
		PrefStore.updateLocale(this);
		setContentView(R.layout.activity_profiles);

		profilesList = (ListView) findViewById(R.id.profilesView);
		adapter = new ArrayAdapter<Profile<String, String>>(this,
				android.R.layout.simple_list_item_single_choice, listItems);
		profilesList.setAdapter(adapter);
		
		profilesList.setOnTouchListener(this);
		
        //initialize the Gesture Detector  
        gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()  
        {  
            @Override  
            public boolean onDoubleTap(MotionEvent e)  
            {  
            	finish();
                return false;  
            }  
        });  
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.updateLocale(getApplicationContext());
		getSupportMenuInflater().inflate(R.menu.activity_profiles, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final EditText input = new EditText(this);
		final int pos = profilesList.getCheckedItemPosition();
		switch (item.getItemId()) {
		case R.id.menu_new:
			new AlertDialog.Builder(this)
					.setTitle(R.string.new_profile_title)
					.setView(input)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String pName = input.getText().toString();
									if (pName.length() > 0
											&& listItems.indexOf(pName) < 0) {
										listItems
												.add(new Profile<String, String>(
														String.valueOf(System
																.currentTimeMillis()),
														pName));
										adapter.notifyDataSetChanged();
									}
								}
							})
					.setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
								}
							}).show();
			break;
		case R.id.menu_edit:
			if (pos >= 0 && pos < listItems.size()) {
				input.setText(listItems.get(pos).getValue());
				input.setSelection(input.getText().length());
				new AlertDialog.Builder(this)
						.setTitle(R.string.edit_profile_title)
						.setView(input)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String pName = input.getText()
												.toString();
										if (pName.length() > 0
												&& listItems.indexOf(pName) < 0) {
											listItems
													.set(pos,
															new Profile<String, String>(
																	listItems
																			.get(pos)
																			.getKey(),
																	pName));
											adapter.notifyDataSetChanged();
										}
									}
								})
						.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										dialog.cancel();
									}
								}).show();
			}
			break;
		case R.id.menu_discard:
			if (pos >= 0 && pos < listItems.size()) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.confirm_discard_title)
						.setMessage(R.string.confirm_discard_message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(android.R.string.yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String fName = listItems.get(pos)
												.getKey();
										listItems.remove(pos);
										int last = listItems.size() - 1;
										if (last >= 0 && pos > last)
											profilesList.setItemChecked(last,
													true);
										adapter.notifyDataSetChanged();
										PrefStore.deleteProfile(
												getApplicationContext(), fName);
									}
								})
						.setNegativeButton(android.R.string.no,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										dialog.cancel();
									}
								}).show();
			}
			break;
		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		PrefStore.setProfiles(getApplicationContext(), listItems);
		int pos = profilesList.getCheckedItemPosition();
		int last = listItems.size() - 1;
		if (pos >= 0 && pos <= last) {
			String profile = listItems.get(pos).getKey();
			if (!PrefStore.CURRENT_PROFILE.equals(profile)) {
				PrefStore.setCurrentProfile(getApplicationContext(), profile);
				PrefStore.PREF_CHANGE = true;
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		this.setTitle(R.string.title_activity_profiles);

		listItems.clear();
		listItems.addAll(PrefStore.getProfiles(getApplicationContext()));
		Collections.sort(listItems, new Comparator<Profile<String, String>>() {
			@Override
			public int compare(Profile<String, String> lhs,
					Profile<String, String> rhs) {
				return lhs.getValue().compareTo(rhs.getValue());
			}
		});
		if (listItems.size() == 0)
			listItems.add(new Profile<String, String>(
					PrefStore.CURRENT_PROFILE, getString(R.string.profile)));
		adapter.notifyDataSetChanged();
		profilesList.setItemChecked(getPosition(PrefStore.CURRENT_PROFILE),
				true);
	}

	private int getPosition(String key) {
		int pos = 0;
		for (Profile<String, String> item : listItems) {
			if (item.getKey().equals(key)) {
				return pos;
			}
			pos++;
		}
		return -1;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gd.onTouchEvent(event);
		return false;
	}

}
