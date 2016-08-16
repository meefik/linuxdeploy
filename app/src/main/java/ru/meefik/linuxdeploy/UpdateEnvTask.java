package ru.meefik.linuxdeploy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.widget.Toast;

public class UpdateEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private Activity activity;

    UpdateEnvTask(Activity activity) {
        this.activity = activity;
        dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(R.string.updating_env_message));
    }

    @Override
    protected void onPreExecute() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return EnvUtils.updateEnv(activity);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (dialog.isShowing()) dialog.dismiss();
        if (!success) {
            Toast.makeText(activity, R.string.toast_updating_env_error, Toast.LENGTH_SHORT).show();
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
