package ru.meefik.linuxdeploy;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

class UpdateEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private Context context;

    UpdateEnvTask(Context c) {
        context = c;
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.updating_env_message));
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return EnvUtils.updateEnv(context);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        try {
            if (dialog.isShowing()) dialog.dismiss();
            if (!success) {
                Toast.makeText(context, R.string.toast_updating_env_error, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
    }
}
