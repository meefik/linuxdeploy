package ru.meefik.linuxdeploy.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.meefik.linuxdeploy.PrefStore;
import ru.meefik.linuxdeploy.R;
import ru.meefik.linuxdeploy.adapter.RepositoryProfileAdapter;
import ru.meefik.linuxdeploy.model.RepositoryProfile;

public class RepositoryActivity extends AppCompatActivity {

    private RepositoryProfileAdapter adapter;

    private boolean isDonated() {
        return getPackageManager().checkSignatures(getPackageName(), "ru.meefik.donate")
                == PackageManager.SIGNATURE_MATCH;
    }

    private void importDialog(final RepositoryProfile repositoryProfile) {
        final String name = repositoryProfile.getProfile();
        final String message = getString(R.string.repository_import_message,
                repositoryProfile.getDescription(),
                repositoryProfile.getSize());

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle(name)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(android.R.string.no, (dialog13, which) -> dialog13.cancel());

        if (isDonated()) {
            dialog.setPositiveButton(R.string.repository_import_button,
                    (dialog1, whichButton) -> importProfile(name));
        } else {
            dialog.setPositiveButton(R.string.repository_purchase_button,
                    (dialog12, whichButton) -> startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=ru.meefik.donate"))));
        }

        dialog.show();
    }

    private void changeUrlDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.edit_text_dialog, null);
        EditText input = view.findViewById(R.id.edit_text);
        input.setText(PrefStore.getRepositoryUrl(this));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle(R.string.repository_change_url_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            String text = input.getText().toString();
                            if (text.isEmpty())
                                text = getString(R.string.repository_url);
                            PrefStore.setRepositoryUrl(getApplicationContext(), text);
                            retrieveIndex();
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> dialog.cancel())
                .show();
    }

    private void retrieveIndex() {
        String url = PrefStore.getRepositoryUrl(this);

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();
        Request request = new Request.Builder()
                .url(url + "/index.gz")
                .build();

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading_message));
        dialog.setCancelable(false);
        dialog.show();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(response.body().byteStream())))) {
                        List<RepositoryProfile> repositoryProfiles = new ArrayList<>();
                        String line;
                        RepositoryProfile repositoryProfile = null;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("PROFILE")) {
                                repositoryProfile = new RepositoryProfile();
                                repositoryProfile.setProfile(line.split("=")[1]);
                            } else if (line.startsWith("DESC")) {
                                repositoryProfile.setDescription(line.split("=")[1]);
                            } else if (line.startsWith("TYPE")) {
                                repositoryProfile.setType(line.split("=")[1]);
                            } else if (line.startsWith("SIZE")) {
                                repositoryProfile.setSize(line.split("=")[1]);
                                repositoryProfiles.add(repositoryProfile);
                            }
                        }

                        runOnUiThread(() -> {
                            adapter.setRepositoryProfiles(repositoryProfiles);
                            dialog.dismiss();
                        });
                    } catch (IOException e) {
                        onFailure();
                    }
                } else {
                    onFailure();
                }
            }

            private void onFailure() {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(RepositoryActivity.this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void importProfile(String name) {
        String url = PrefStore.getRepositoryUrl(this);

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();
        Request request = new Request.Builder()
                .url(url + "/index.gz")
                .build();

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading_message));
        dialog.setCancelable(false);
        dialog.show();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                onFailure();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    String conf = PrefStore.getEnvDir(RepositoryActivity.this) + "/config/" + name + ".conf";
                    try (OutputStream os = new FileOutputStream(conf)) {
                        os.write(response.body().bytes());

                        runOnUiThread(dialog::dismiss);
                        PrefStore.changeProfile(RepositoryActivity.this, name);
                        finish();
                    } catch (IOException e) {
                        onFailure();
                    }
                } else {
                    onFailure();
                }
            }

            private void onFailure() {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(RepositoryActivity.this, R.string.toast_loading_error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefStore.setLocale(this);
        setContentView(R.layout.activity_repository);

        // RecyclerView Adapter
        RecyclerView recyclerView = findViewById(R.id.repositoryView);
        adapter = new RepositoryProfileAdapter();
        adapter.setOnItemClickListener(this::importDialog);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Load list
        retrieveIndex();
    }

    @Override
    public void setTheme(int resId) {
        super.setTheme(PrefStore.getTheme(this));
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitle(R.string.title_activity_repository);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        PrefStore.setLocale(this);
        getMenuInflater().inflate(R.menu.activity_repository, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                retrieveIndex();
                break;
            case R.id.menu_change_url:
                changeUrlDialog();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
}
