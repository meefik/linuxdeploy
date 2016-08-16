package ru.meefik.linuxdeploy;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MountsActivity extends AppCompatActivity {

    private List<String> listItems = new ArrayList<>();
    private ArrayAdapter adapter;

    private void addDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_mount_title)
                .setView(input, 16, 32, 16, 0)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                String text = input.getText().toString()
                                        .replaceAll(" ", "_");
                                if (text.length() > 0) {
                                    listItems.add(text);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }).setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,
                                        int whichButton) {
                        dialog.cancel();
                    }
                }).show();
    }

    private void editDialog(final int position) {
        final EditText input = new EditText(this);
        if (position >= 0 && position < listItems.size()) {
            input.setText(listItems.get(position));
            input.setSelection(input.getText().length());
            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_mount_title)
                    .setView(input, 16, 32, 16, 0)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    String text = input.getText()
                                            .toString()
                                            .replaceAll(" ", "_");
                                    if (text.length() > 0) {
                                        listItems.set(position, text);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }).setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            dialog.cancel();
                        }
                    }).show();
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
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                    listItems.remove(position);
                                    adapter.notifyDataSetChanged();
                                }
                            }).setNegativeButton(android.R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            dialog.cancel();
                        }
                    }).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_mounts);

        // ListView Adapter
        ListView listView = (ListView) findViewById(R.id.mountsView);
        adapter = new ArrayAdapter<String>(this, R.layout.mounts_row, listItems) {
            @Override
            public View getView(final int position, View view, final ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getApplicationContext().
                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.mounts_row, null);
                }
                String item = getItem(position);

                ((TextView) view.findViewById(R.id.mount_point)).setText(item);

                view.findViewById(R.id.mount_point).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                    }
                });

                view.findViewById(R.id.delete_mount).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                    }
                });

                return view;
            }
        };
        listView.setAdapter(adapter);

        // Click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long viewId = view.getId();
                if (viewId == R.id.delete_mount) deleteDialog(position);
                else editDialog(position);
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
