package ru.meefik.linuxdeploy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;

class RemoveEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private Activity activity;

    RemoveEnvTask(Activity activity) {
        this.activity = activity;
        dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(R.string.removing_env_message));
    }

    @Override
    protected void onPreExecute() {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return EnvUtils.removeEnv(activity);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (dialog.isShowing()) dialog.dismiss();
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }
}
