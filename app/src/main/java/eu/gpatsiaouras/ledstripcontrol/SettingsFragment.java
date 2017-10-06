package eu.gpatsiaouras.ledstripcontrol;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener{

    private static final String PATTER_IP =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_general);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int count = prefScreen.getPreferenceCount();

        // Go through all of the preferences, and set up their preference summary.
        for (int i = 0; i < count; i++) {
            Preference p = prefScreen.getPreference(i);
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }

        Preference preferencePort = findPreference(getString(R.string.pref_port_key));
        Preference preferenceIp = findPreference(getString(R.string.pref_ip_key));
        preferenceIp.setOnPreferenceChangeListener(this);
        preferencePort.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Figure out which preference was changed
        Preference preference = findPreference(key);
        if (null != preference) {
            // Updates the summary for the preference
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        Toast errorPort = Toast.makeText(getContext(), "Please select a number between 1 - 99999", Toast.LENGTH_SHORT);
        Toast errorIp = Toast.makeText(getContext(), "Please enter a valid ip address XXX.XXX.XXX.XXX", Toast.LENGTH_SHORT);

        String portKey = getString(R.string.pref_port_key);
        String ipKey = getString(R.string.pref_ip_key);
        if (preference.getKey().equals(portKey)){
            String stringPort = (String) newValue;
            try {
                int size = Integer.parseInt(stringPort);

                if (size > 99999 || size <= 0) {
                    errorPort.show();
                    return false;
                }
            } catch (NumberFormatException e) {
                errorPort.show();
                return false;
            }
        } else if (preference.getKey().equals(ipKey)){
            String stringIp = (String) newValue;
            if (!Patterns.IP_ADDRESS.matcher(stringIp).matches()) {
                errorIp.show();
                return false;
            }
        }
        return true;
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            // For list preferences, figure out the label of the selected value
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                // Set the summary to that label
                Log.d(SettingsFragment.class.getSimpleName(), "RE malakes");
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            // For EditTextPreferences, set the summary to the value's simple string representation.
            preference.setSummary(value);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
