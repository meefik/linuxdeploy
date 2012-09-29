package ru.meefik.linuxdeploy;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;


public class PreferencesActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,Preference.OnPreferenceClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        AppPrefs.updateLocale(getBaseContext());
        
        addPreferencesFromResource(R.xml.preferences);
        
        ListPreference language = (ListPreference)this.findPreference("language");
        language.setOnPreferenceChangeListener(this);
        
        EditTextPreference fontsize = (EditTextPreference)this.findPreference("fontsize");
        fontsize.setOnPreferenceChangeListener(this);
        
        EditTextPreference customscript = (EditTextPreference)this.findPreference("customscript");
        customscript.setOnPreferenceChangeListener(this);
        
        EditTextPreference mountpath = (EditTextPreference)this.findPreference("mountpath");
        mountpath.setOnPreferenceChangeListener(this);
        
        EditTextPreference installdir = (EditTextPreference)this.findPreference("installdir");
        installdir.setOnPreferenceChangeListener(this);
        
        EditTextPreference diskimage = (EditTextPreference)this.findPreference("diskimage");
        diskimage.setOnPreferenceChangeListener(this);
        
        EditTextPreference disksize = (EditTextPreference)this.findPreference("disksize");
        disksize.setOnPreferenceChangeListener(this);
        
        ListPreference architecture = (ListPreference)this.findPreference("architecture");
        architecture.setOnPreferenceChangeListener(this);
        
        ListPreference suite = (ListPreference)this.findPreference("suite");
        suite.setOnPreferenceChangeListener(this);
        
        EditTextPreference mirror = (EditTextPreference)this.findPreference("mirror");
        mirror.setOnPreferenceChangeListener(this);
        
        EditTextPreference username = (EditTextPreference)this.findPreference("username");
        username.setOnPreferenceChangeListener(this);
        
        EditTextPreference sshport = (EditTextPreference)this.findPreference("sshport");
        sshport.setOnPreferenceChangeListener(this);
        
        EditTextPreference vncdisplay = (EditTextPreference)this.findPreference("vncdisplay");
        vncdisplay.setOnPreferenceChangeListener(this);
        
        ListPreference vncdepth = (ListPreference)this.findPreference("vncdepth");
        vncdepth.setOnPreferenceChangeListener(this);
        
        EditTextPreference vncwidth = (EditTextPreference)this.findPreference("vncwidth");
        vncwidth.setOnPreferenceChangeListener(this);
        
        EditTextPreference vncheight = (EditTextPreference)this.findPreference("vncheight");
        vncheight.setOnPreferenceChangeListener(this);
        
        EditTextPreference logfile = (EditTextPreference)this.findPreference("logfile");
        logfile.setOnPreferenceChangeListener(this);
        

        PreferenceScreen installenv = (PreferenceScreen)this.findPreference("installenv");
        installenv.setOnPreferenceClickListener(this);

        PreferenceScreen reset = (PreferenceScreen)this.findPreference("reset");
        reset.setOnPreferenceClickListener(this);
        
        PreferenceScreen reconfigure = (PreferenceScreen)this.findPreference("reconfigure");
        reconfigure.setOnPreferenceClickListener(this);

    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("language")) {
			ListPreference language = (ListPreference)this.findPreference("language");
			language.setValue((String)newValue);
			language.setSummary(language.getEntry());
		} else {
			preference.setSummary((CharSequence)newValue);
		}
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("installenv")) {
			updateEnvDialog();
		}
		if (preference.getKey().equals("reset")) {
			resetSettingsDialog();
		}
		if (preference.getKey().equals("reconfigure")) {
			reconfigureDialog();
		}
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
        ListPreference language = (ListPreference)this.findPreference("language");
        language.setSummary(language.getEntry());
        
        EditTextPreference fontsize = (EditTextPreference)this.findPreference("fontsize");
        fontsize.setSummary(fontsize.getText());
        
        EditTextPreference customscript = (EditTextPreference)this.findPreference("customscript");
        customscript.setSummary(customscript.getText());
        
        EditTextPreference mountpath = (EditTextPreference)this.findPreference("mountpath");
        mountpath.setSummary(mountpath.getText());
        
        EditTextPreference installdir = (EditTextPreference)this.findPreference("installdir");
        installdir.setSummary(installdir.getText());
        
        EditTextPreference diskimage = (EditTextPreference)this.findPreference("diskimage");
        diskimage.setSummary(diskimage.getText());
        
        EditTextPreference disksize = (EditTextPreference)this.findPreference("disksize");
        disksize.setSummary(disksize.getText());
        
        ListPreference architecture = (ListPreference)this.findPreference("architecture");
        architecture.setSummary(architecture.getValue());
        
        ListPreference suite = (ListPreference)this.findPreference("suite");
        suite.setSummary(suite.getValue());
        
        EditTextPreference mirror = (EditTextPreference)this.findPreference("mirror");
        mirror.setSummary(mirror.getText());
        
        EditTextPreference username = (EditTextPreference)this.findPreference("username");
        username.setSummary(username.getText());
        
        EditTextPreference sshport = (EditTextPreference)this.findPreference("sshport");
        sshport.setSummary(sshport.getText());
        
        EditTextPreference vncdisplay = (EditTextPreference)this.findPreference("vncdisplay");
        vncdisplay.setSummary(vncdisplay.getText());
        
        ListPreference vncdepth = (ListPreference)this.findPreference("vncdepth");
        vncdepth.setSummary(vncdepth.getValue());
        
        EditTextPreference vncwidth = (EditTextPreference)this.findPreference("vncwidth");
        vncwidth.setSummary(vncwidth.getText());
        
        EditTextPreference vncheight = (EditTextPreference)this.findPreference("vncheight");
        vncheight.setSummary(vncheight.getText());
        
        EditTextPreference logfile = (EditTextPreference)this.findPreference("logfile");
        logfile.setSummary(logfile.getText());
        
	}
	
	@Override
	public void onPause() {
		super.onPause();

	}
	
	private void updateEnvDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.title_installenv_preference)
		.setMessage(R.string.message_installenv_confirm_dialog)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
		        (new Thread() {
		        	public void run() {
		        		new ShellEnv(getBaseContext()).updateEnv();
		        		new ShellEnv(getBaseContext()).updateConfig();
		        	}
		        }).start();
				finish();
		    }
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		    	dialog.cancel();
		    }
		}).show();
	}
	
	private void resetSettingsDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.title_reset_preference)
		.setMessage(R.string.message_reset_confirm_dialog)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor editor = sp.edit();
				editor.clear();
				PreferenceManager.setDefaultValues(getBaseContext(), R.xml.preferences, false);
				editor.commit();
				finish();
		    }
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		    	dialog.cancel();
		    }
		}).show();
	}
	
	private void reconfigureDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.title_reconfigure_preference)
		.setMessage(R.string.message_reconfigure_confirm_dialog)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setCancelable(false)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				new ShellEnv(getBaseContext()).DeployCmd("config");
				finish();
		    }
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		    	dialog.cancel();
		    }
		}).show();
	}
	
}
