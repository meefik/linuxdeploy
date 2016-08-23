package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfilesActivity extends AppCompatActivity implements OnTouchListener {

    private ListView listView;
    private List<String> listItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private GestureDetector gd;

    /**
     * Get position by key
     *
     * @param key
     * @return position
     */
    private int getPosition(String key) {
        int pos = 0;
        for (String item : listItems) {
            if (item.equals(key)) return pos;
            pos++;
        }
        return -1;
    }

    /**
     * Rename conf file associated with the profile
     *
     * @param c context
     * @param oldName old profile name
     * @param newName new profile name
     * @return true if success
     */
    public static boolean renameConf(Context c, String oldName, String newName) {
        File oldFile = new File(PrefStore.getEnvDir(c) + "/config/" + oldName + ".conf");
        File newFile = new File(PrefStore.getEnvDir(c) + "/config/" + newName + ".conf");
        return oldFile.renameTo(newFile);
    }

    /**
     * Remove conf file associated with the profile
     *
     * @param c    context
     * @param name profile name
     * @return true if success
     */
    public static boolean removeConf(Context c, String name) {
        File confFile = new File(PrefStore.getEnvDir(c) + "/config/" + name + ".conf");
        return confFile.exists() && confFile.delete();
    }

    /**
     * Get list of profiles
     *
     * @param c context
     * @return list of profiles
     */
    public static List<String> getProfiles(Context c) {
        List<String> profiles = new ArrayList<>();
        File confDir = new File(PrefStore.getEnvDir(c) + "/config");
        File[] listOfFiles = confDir.listFiles();
        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    String filename = listOfFile.getName();
                    int index = filename.lastIndexOf('.');
                    if (index != -1) filename = filename.substring(0, index);
                    profiles.add(filename);
                }
            }
        }
        return profiles;
    }

    private void addDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_profile_title)
                .setView(input, 16, 32, 16, 0)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String text = input.getText().toString();
                                if (text.length() > 0) {
                                    listItems.add(text.replaceAll("[^A-Za-z0-9_\\-]", "_"));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        }).show();
    }

    private void editDialog() {
        final EditText input = new EditText(this);
        final int pos = listView.getCheckedItemPosition();
        if (pos >= 0 && pos < listItems.size()) {
            final String profileOld = listItems.get(pos);
            input.setText(profileOld);
            input.setSelection(input.getText().length());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_profile_title)
                    .setView(input, 16, 32, 16, 0)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String text = input.getText().toString();
                                    if (text.length() > 0) {
                                        String profileNew = text.replaceAll("[^A-Za-z0-9_\\-]", "_");
                                        if (!profileOld.equals(profileNew)) {
                                            renameConf(getApplicationContext(), profileOld, profileNew);
                                            listItems.set(pos, profileNew);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    private void deleteDialog() {
        final int pos = listView.getCheckedItemPosition();
        if (pos >= 0 && pos < listItems.size()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_profile_discard_title)
                    .setMessage(R.string.confirm_profile_discard_message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String key = listItems.get(pos);
                                    listItems.remove(pos);
                                    int last = listItems.size() - 1;
                                    if (last < 0) listItems.add(getString(R.string.profile));
                                    if (last >= 0 && pos > last)
                                        listView.setItemChecked(last, true);
                                    adapter.notifyDataSetChanged();
                                    removeConf(getApplicationContext(), key);
                                }
                            })
                    .setNegativeButton(android.R.string.no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            }).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_profiles);

        // ListView Adapter
        listView = (ListView) findViewById(R.id.profilesView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, listItems);
        listView.setAdapter(adapter);

        // Initialize the Gesture Detector
        listView.setOnTouchListener(this);
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
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PrefStore.setLocale(this);
        getMenuInflater().inflate(R.menu.activity_profiles, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                addDialog();
                break;
            case R.id.menu_edit:
                editDialog();
                break;
            case R.id.menu_delete:
                deleteDialog();
                break;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        int pos = listView.getCheckedItemPosition();
        if (pos >= 0 && pos < listItems.size()) {
            String profile = listItems.get(pos);
            PrefStore.changeProfile(this, profile);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.title_activity_profiles);
        listItems.clear();
        listItems.addAll(getProfiles(this));
        Collections.sort(listItems);
        String profile = PrefStore.getProfileName(this);
        if (listItems.size() == 0) listItems.add(profile);
        adapter.notifyDataSetChanged();
        listView.setItemChecked(getPosition(profile), true);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gd.onTouchEvent(event);
        return false;
    }

}
