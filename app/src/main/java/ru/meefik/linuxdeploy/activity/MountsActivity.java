package ru.meefik.linuxdeploy.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.adapter.MountAdapter;
import ru.meefik.linuxdeploy.model.Mount;

public class MountsActivity extends AppCompatActivity {

    private MountAdapter adapter;

    private void addDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.properties_mounts, null);
        EditText inputSrc = view.findViewById(R.id.editTextSrc);
        EditText inputTarget = view.findViewById(R.id.editTextTarget);

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_mount_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            String src = inputSrc.getText().toString()
                                    .replaceAll("[ :]", "_");
                            String target = inputTarget.getText().toString()
                                    .replaceAll("[ :]", "_");
                            if (!src.isEmpty()) {
                                adapter.addMount(new Mount(src, target));
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> dialog.cancel()).show();
    }

    private void editDialog(Mount mount) {
        View view = LayoutInflater.from(this).inflate(R.layout.properties_mounts, null);
        EditText inputSrc = view.findViewById(R.id.editTextSrc);
        EditText inputTarget = view.findViewById(R.id.editTextTarget);

        inputSrc.setText(mount.getSource());
        inputSrc.setSelection(mount.getSource().length());

        inputTarget.setText(mount.getTarget());
        inputTarget.setSelection(mount.getTarget().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.edit_mount_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            String src = inputSrc.getText().toString()
                                    .replaceAll("[ :]", "_");
                            String target = inputTarget.getText().toString()
                                    .replaceAll("[ :]", "_");
                            if (!src.isEmpty()) {
                                mount.setSource(src);
                                mount.setTarget(target);
                                adapter.notifyDataSetChanged();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> dialog.cancel())
                .show();
    }

    private void deleteDialog(Mount mount) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_mount_discard_title)
                .setMessage(R.string.confirm_mount_discard_message)
                .setIcon(R.drawable.ic_warning_24dp)
                .setPositiveButton(android.R.string.yes,
                        (dialog, whichButton) -> adapter.removeMount(mount))
                .setNegativeButton(android.R.string.no,
                        (dialog, whichButton) -> dialog.cancel())
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_mounts);

        // RecyclerView Adapter
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        adapter = new MountAdapter();
        adapter.setOnItemClickListener(this::editDialog);
        adapter.setOnItemDeleteListener(this::deleteDialog);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
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
        if (item.getItemId() == R.id.menu_add) {
            addDialog();
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String titleMsg = getString(R.string.title_activity_mounts) + ": "
                + PrefStore.getProfileName(this);
        setTitle(titleMsg);

        adapter.setMounts(PrefStore.getMountsList(this));
    }

    @Override
    public void onPause() {
        super.onPause();

        PrefStore.setMountsList(this, adapter.getMounts());
    }
}
