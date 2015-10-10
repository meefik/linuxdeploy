package ru.meefik.linuxdeploy;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class ProfilesActivity extends SherlockActivity implements
		OnTouchListener {

	private ListView listView;
	private List<Profile<String, String>> listItems = new ArrayList<Profile<String, String>>();
	private ArrayAdapter<Profile<String, String>> adapter;
	private GestureDetector gd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PrefStore.setLocale(this);
		setContentView(R.layout.activity_profiles);

		listView = (ListView) findViewById(R.id.profilesView);
		adapter = new ArrayAdapter<Profile<String, String>>(this,
				android.R.layout.simple_list_item_single_choice, listItems);
		listView.setAdapter(adapter);

		listView.setOnTouchListener(this);

		// initialize the Gesture Detector
		gd = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onDoubleTap(MotionEvent e) {
						finish();
						return false;
					}
				});
	}
	
    @Override
    public void setTheme(int resid) {
        super.setTheme(PrefStore.getTheme(this));
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		PrefStore.setLocale(this);
		getSupportMenuInflater().inflate(R.menu.activity_profiles, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final EditText input = new EditText(this);
		final int pos = listView.getCheckedItemPosition();
		File extStore = Environment.getExternalStorageDirectory();
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
									String text = input.getText().toString();
									if (text.length() > 0) {
										listItems
												.add(new Profile<String, String>(
														String.valueOf(System
																.currentTimeMillis()),
														text));
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
										String text = input.getText()
												.toString();
										if (text.length() > 0) {
											listItems
													.set(pos,
															new Profile<String, String>(
																	listItems
																			.get(pos)
																			.getKey(),
																	text));
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
						.setTitle(R.string.confirm_profile_discard_title)
						.setMessage(R.string.confirm_profile_discard_message)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setCancelable(false)
						.setPositiveButton(android.R.string.yes,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String key = listItems.get(pos)
												.getKey();
										listItems.remove(pos);
										int last = listItems.size() - 1;
										if (last >= 0 && pos > last)
											listView.setItemChecked(last, true);
										adapter.notifyDataSetChanged();
										PrefStore.deleteProfile(
												getApplicationContext(), key);
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
		case R.id.menu_import:
			String srcFile = extStore.getAbsolutePath() + "/linux.xml";
			input.setText(srcFile);
			input.setSelection(input.getText().length());
			new AlertDialog.Builder(this)
					.setTitle(R.string.import_profile_title)
					.setView(input)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String profilePath = input.getText()
											.toString();
									String key = String.valueOf(System
											.currentTimeMillis());
									if (PrefStore.importProfile(
											getApplicationContext(), key,
											profilePath)) {
										String profileFile = new File(
												profilePath).getName();
										String profileName = profileFile
												.replaceAll("^(.*).xml$", "$1");

										listItems
												.add(new Profile<String, String>(
														key, profileName));
										adapter.notifyDataSetChanged();

										Toast toast = Toast
												.makeText(
														getApplicationContext(),
														getString(R.string.toast_import_profile_success),
														Toast.LENGTH_SHORT);
										toast.show();
									} else {
										Toast toast = Toast
												.makeText(
														getApplicationContext(),
														getString(R.string.toast_import_profile_error),
														Toast.LENGTH_SHORT);
										toast.show();
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
		case R.id.menu_export:
			if (pos >= 0 && pos < listItems.size()) {
				String validFileName = listItems.get(pos).getValue()
						.replaceAll("[^0-9a-zA-Z_-]", "_");
				String dstFile = extStore.getAbsolutePath() + "/"
						+ validFileName + ".xml";
				input.setText(dstFile);
				input.setSelection(input.getText().length());
				new AlertDialog.Builder(this)
						.setTitle(R.string.export_profile_title)
						.setView(input)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String pName = input.getText()
												.toString();
										if (PrefStore.exportProfile(
												getApplicationContext(),
												listItems.get(pos).getKey(),
												pName)) {
											Toast toast = Toast
													.makeText(
															getApplicationContext(),
															getString(R.string.toast_export_profile_success),
															Toast.LENGTH_SHORT);
											toast.show();
										} else {
											Toast toast = Toast
													.makeText(
															getApplicationContext(),
															getString(R.string.toast_export_profile_error),
															Toast.LENGTH_SHORT);
											toast.show();
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
		}
		return false;
	}

	@Override
	public void onPause() {
		super.onPause();
		PrefStore.setProfiles(getApplicationContext(), listItems);
		int pos = listView.getCheckedItemPosition();
		int last = listItems.size() - 1;
		if (pos >= 0 && pos <= last) {
			String profile = listItems.get(pos).getKey();
			if (!PrefStore.getCurrentProfile(this).equals(profile)) {
				PrefStore.setCurrentProfile(getApplicationContext(), profile);
				PrefStore.CONF_CHANGE = true;
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		setTitle(R.string.title_activity_profiles);
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
					PrefStore.getCurrentProfile(this), getString(R.string.profile)));
		adapter.notifyDataSetChanged();
		listView.setItemChecked(getPosition(PrefStore.getCurrentProfile(this)), true);
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
