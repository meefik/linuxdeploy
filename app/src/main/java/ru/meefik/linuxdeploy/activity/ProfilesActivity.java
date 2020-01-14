package ru.meefik.linuxdeploy.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;

public class ProfilesActivity extends AppCompatActivity implements OnTouchListener {

    private ListView listView;
    private List<String> listItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private GestureDetector gd;

    /**
     * Rename conf file associated with the profile
     *
     * @param c       context
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
        File[] profileFiles = confDir.listFiles();

        if (profileFiles != null) {
            for (File profileFile : profileFiles) {
                if (profileFile.isFile()) {
                    String filename = profileFile.getName();
                    int index = filename.lastIndexOf('.');
                    if (index != -1) filename = filename.substring(0, index);
                    profiles.add(filename);
                }
            }
        }

        return profiles;
    }

    /**
     * Get position by key
     *
     * @param key
     * @return position
     */
    private int getPosition(String key) {
        for (int i = 0; i < listItems.size(); i++) {
            if (listItems.get(i).equals(key))
                return i;
        }

        return -1;
    }

    private void addDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.edit_text_dialog, null);
        EditText input = view.findViewById(R.id.edit_text);

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_profile_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            String text = input.getText().toString();
                            if (!text.isEmpty()) {
                                listItems.add(text.replaceAll("[^A-Za-z0-9_\\-]", "_"));
                                adapter.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> dialog.cancel())
                .show();
    }

    private void editDialog() {
        int pos = listView.getCheckedItemPosition();
        if (pos >= 0 && pos < listItems.size()) {
            String profileOld = listItems.get(pos);

            View view = LayoutInflater.from(this).inflate(R.layout.edit_text_dialog, null);
            EditText input = view.findViewById(R.id.edit_text);
            input.setText(profileOld);
            input.setSelection(input.getText().length());

            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_profile_title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, whichButton) -> {
                                String text = input.getText().toString();
                                if (!text.isEmpty()) {
                                    String profileNew = text.replaceAll("[^A-Za-z0-9_\\-]", "_");
                                    if (!profileOld.equals(profileNew)) {
                                        renameConf(getApplicationContext(), profileOld, profileNew);
                                        listItems.set(pos, profileNew);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, whichButton) -> dialog.cancel())
                    .show();
        }
    }

    private void deleteDialog() {
        final int pos = listView.getCheckedItemPosition();
        if (pos >= 0 && pos < listItems.size()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_profile_discard_title)
                    .setMessage(R.string.confirm_profile_discard_message)
                    .setIcon(R.drawable.ic_warning_24dp)
                    .setPositiveButton(android.R.string.yes,
                            (dialog, whichButton) -> {
                                String key = listItems.remove(pos);
                                int last = listItems.size() - 1;
                                if (last < 0) listItems.add(getString(R.string.profile));
                                if (last >= 0 && pos > last)
                                    listView.setItemChecked(last, true);
                                adapter.notifyDataSetChanged();
                                removeConf(getApplicationContext(), key);
                            })
                    .setNegativeButton(android.R.string.no,
                            (dialog, whichButton) -> dialog.cancel())
                    .show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_profiles);

        // ListView Adapter
        listView = findViewById(R.id.profilesView);
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
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
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
