package ru.meefik.linuxdeploy;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class UpdateEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private WeakReference<Context> contextWeakReference;

    public UpdateEnvTask(Context c) {
        contextWeakReference = new WeakReference<>(c);
    }

    @Override
    protected void onPreExecute() {
        Context context = contextWeakReference.get();
        if (context != null) {
            dialog = new ProgressDialog(context);
            dialog.setMessage(context.getString(R.string.updating_env_message));
            dialog.show();
        }
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Context context = contextWeakReference.get();
        return context != null ? EnvUtils.updateEnv(context) : null;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        Context context = contextWeakReference.get();
        if (context != null) {
            if (dialog.isShowing()) dialog.dismiss();
            if (!success) {
                Toast.makeText(context, R.string.toast_updating_env_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
