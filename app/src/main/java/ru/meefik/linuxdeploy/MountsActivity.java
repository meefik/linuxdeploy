package ru.meefik.linuxdeploy;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MountsActivity extends AppCompatActivity {

    private List<String> listItems = new ArrayList<>();
    private ArrayAdapter adapter;

    private void addDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.properties_mounts, null);
        final EditText inputSrc = view.findViewById(R.id.editTextSrc);
        final EditText inputTarget = view.findViewById(R.id.editTextTarget);
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_mount_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            String text = "";
                            String src = inputSrc.getText().toString()
                                    .replaceAll("[ :]", "_");
                            String target = inputTarget.getText().toString()
                                    .replaceAll("[ :]", "_");
                            if (src.length() > 0) {
                                text = src;
                                if (target.length() > 0) {
                                    text = text + ":" + target;
                                }
                            }
                            if (text.length() > 0) {
                                listItems.add(text);
                                adapter.notifyDataSetChanged();
                            }
                        }).setNegativeButton(android.R.string.cancel,
                (dialog, whichButton) -> dialog.cancel()).show();
    }

    private void editDialog(final int position) {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.properties_mounts, null);
        final EditText inputSrc = view.findViewById(R.id.editTextSrc);
        final EditText inputTarget = view.findViewById(R.id.editTextTarget);
        if (position >= 0 && position < listItems.size()) {
            String text = listItems.get(position);
            final String[] arr = text.split(":", 2);
            try {
                inputSrc.setText(arr[0]);
                inputSrc.setSelection(arr[0].length());

                inputTarget.setText(arr[1]);
                inputTarget.setSelection(arr[1].length());
            } catch (IndexOutOfBoundsException ignored) {
            }

            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_mount_title)
                    .setView(view)
                    .setPositiveButton(android.R.string.ok,
                            (dialog, whichButton) -> {
                                String text1 = "";
                                String src = inputSrc.getText().toString()
                                        .replaceAll("[ :]", "_");
                                String target = inputTarget.getText().toString()
                                        .replaceAll("[ :]", "_");
                                if (src.length() > 0) {
                                    text1 = src;
                                    if (target.length() > 0) {
                                        text1 = text1 + ":" + target;
                                    }
                                }
                                if (text1.length() > 0) {
                                    listItems.set(position, text1);
                                    adapter.notifyDataSetChanged();
                                }
                            }).setNegativeButton(android.R.string.cancel,
                    (dialog, whichButton) -> dialog.cancel()).show();
        }
    }

    private void deleteDialog(final int position) {
        if (position >= 0 && position < listItems.size()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_mount_discard_title)
                    .setMessage(R.string.confirm_mount_discard_message)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes,
                            (dialog, whichButton) -> {
                                listItems.remove(position);
                                adapter.notifyDataSetChanged();
                            }).setNegativeButton(android.R.string.no,
                    (dialog, whichButton) -> dialog.cancel()).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_mounts);

        // ListView Adapter
        ListView listView = findViewById(R.id.mountsView);
        adapter = new ArrayAdapter<String>(this, R.layout.mounts_row, R.id.mount_point, listItems) {
            @Override
            public View getView(final int position, View convertView, final ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(R.id.mount_point);
                Button btn = view.findViewById(R.id.delete_mount);

                String item = getItem(position);
                tv.setText(item);

                tv.setOnClickListener(v -> {
                    ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                });

                btn.setOnClickListener(v -> {
                    ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                });

                return view;
            }
        };
        listView.setAdapter(adapter);

        // Click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            long viewId = view.getId();
            if (viewId == R.id.delete_mount) deleteDialog(position);
            else editDialog(position);
        });
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PrefStore.setLocale(this);
        getMenuInflater().inflate(R.menu.activity_mounts, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                addDialog();
                break;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String titleMsg = getString(R.string.title_activity_mounts) + ": "
                + PrefStore.getProfileName(this);
        setTitle(titleMsg);

        listItems.addAll(PrefStore.getMountsList(this));
    }

    @Override
    public void onPause() {
        super.onPause();

        PrefStore.setMountsList(this, listItems);
    }
}
