package ru.meefik.linuxdeploy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.view.Surface;
import android.widget.Toast;

class UpdateEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private Activity activity;
    private int orientation;

    UpdateEnvTask(Activity activity) {
        this.activity = activity;
        dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(R.string.updating_env_message));
    }

    @Override
    protected void onPreExecute() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_180:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_0:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
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
