package ru.meefik.linuxdeploy;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

class RemoveEnvTask extends AsyncTask<String, Void, Boolean> {

    private ProgressDialog dialog;
    private Context context;

    RemoveEnvTask(Context c) {
        context = c;
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getString(R.string.removing_env_message));
    }

    @Override
    protected void onPreExecute() {
        dialog.show();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return EnvUtils.removeEnv(context);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        try {
            if (dialog.isShowing()) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }
}
