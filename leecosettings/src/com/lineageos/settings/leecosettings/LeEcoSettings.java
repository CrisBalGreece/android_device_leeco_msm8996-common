/*
* Copyright (C) 2022 The Android Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/

package com.lineageos.settings.leecosettings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;

import android.util.Log;
import android.os.SystemProperties;
import java.io.*;
import android.widget.Toast;
import android.preference.ListPreference;

import com.lineageos.settings.leecosettings.R;

public class LeEcoSettings extends PreferenceActivity implements OnPreferenceChangeListener {
    private static final boolean DEBUG = false;
    private static final String TAG = "LeEcoSettings";
    private static final String SPECTRUM_KEY = "spectrum";
    private static final String ENABLE_HAL3_KEY = "hal3";
    private static final String THERMAL_KEY = "thermal";
    private static final String SPECTRUM_SYSTEM_PROPERTY = "persist.spectrum.profile";
    private static final String CDLA_SYSTEM_PROPERTY = "persist.cdla_enable";
    private static final String QC_SYSTEM_PROPERTY = "persist.sys.le_fast_chrg_enable";
    private static final String SYSTEM_PROPERTY_CAMERA_FOCUS_FIX = "persist.camera.focus_fix";	
    private static final String HAL3_SYSTEM_PROPERTY = "persist.camera.HAL3.enabled";
    private static final String THERMAL_SYSTEM_PROPERTY = "persist.thermal.profile";
    private Preference mKcalPref;
    private ListPreference mSPECTRUM;
    private ListPreference mTHERMAL;
    private SwitchPreference mEnableQC;
    private SwitchPreference mCameraFocusFix;
    private SwitchPreference mEnableCDLA;
    private SwitchPreference mEnableHAL3;


    private Preference mSaveLog;

    private Context mContext;
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.leeco_settings);
        mKcalPref = findPreference("kcal");
                mKcalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                     @Override
                     public boolean onPreferenceClick(Preference preference) {
                         Intent intent = new Intent(getApplicationContext(), DisplayCalibration.class);
                         startActivity(intent);
                         return true;
                     }
                });
        mContext = getApplicationContext();

        mEnableQC = (SwitchPreference) findPreference(QC_SYSTEM_PROPERTY);
        if( mEnableQC != null ) {
            mEnableQC.setChecked(SystemProperties.getBoolean(QC_SYSTEM_PROPERTY, false));
            mEnableQC.setOnPreferenceChangeListener(this);
        }

        mEnableCDLA = (SwitchPreference) findPreference(CDLA_SYSTEM_PROPERTY);
        if( mEnableCDLA != null ) {
            mEnableCDLA.setChecked(SystemProperties.getBoolean(CDLA_SYSTEM_PROPERTY, false));
            mEnableCDLA.setOnPreferenceChangeListener(this);
        }

        mCameraFocusFix = (SwitchPreference) findPreference(SYSTEM_PROPERTY_CAMERA_FOCUS_FIX);
        if( mCameraFocusFix != null ) {
            mCameraFocusFix.setChecked(SystemProperties.getBoolean(SYSTEM_PROPERTY_CAMERA_FOCUS_FIX, false));
            mCameraFocusFix.setOnPreferenceChangeListener(this);
        }
		
        mEnableHAL3 = (SwitchPreference) findPreference(ENABLE_HAL3_KEY);
        if( mEnableHAL3 != null ) {
            mEnableHAL3.setChecked(SystemProperties.getBoolean(HAL3_SYSTEM_PROPERTY, false));
            mEnableHAL3.setOnPreferenceChangeListener(this);
        }

        mSPECTRUM = (ListPreference) findPreference(SPECTRUM_KEY);
        if( mSPECTRUM != null ) {
            mSPECTRUM.setValue(SystemProperties.get(SPECTRUM_SYSTEM_PROPERTY, "0"));
            mSPECTRUM.setOnPreferenceChangeListener(this);
        }
		
	mTHERMAL = (ListPreference) findPreference(THERMAL_KEY);
        if( mTHERMAL != null ) {
            mTHERMAL.setValue(SystemProperties.get(THERMAL_SYSTEM_PROPERTY, "0"));
            mTHERMAL.setOnPreferenceChangeListener(this);
        }

}

    // Camera2API
    private void setEnableHAL3(boolean value) {
	if(value) {
		SystemProperties.set(HAL3_SYSTEM_PROPERTY, "1");
	} else {
		SystemProperties.set(HAL3_SYSTEM_PROPERTY, "0");
	}
	if (DEBUG) Log.d(TAG, "HAL3 setting changed");
    }
    
    // SPECTRUM SETTINGS
    private void setSPECTRUM(String value) {
		SystemProperties.set(SPECTRUM_SYSTEM_PROPERTY, value);
    }
    
    // THERMAL SETTINGS
    private void setTHERMAL(String value) {
		SystemProperties.set(THERMAL_SYSTEM_PROPERTY, value);
    }

    private void setEnable(String key, boolean value) {
	if(value) {
 	      SystemProperties.set(key, "1");
    	} else {
    		SystemProperties.set(key, "0");
    	}
    	if (DEBUG) Log.d(TAG, key + " setting changed");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        boolean value;
        String strvalue;
        if (DEBUG) Log.d(TAG, "Preference changed.");
	if (SPECTRUM_KEY.equals(key)) {
		strvalue = (String) newValue;
		if (DEBUG) Log.d(TAG, "SPECTRUM setting changed: " + strvalue);
		setSPECTRUM(strvalue);
		return true;
        } else if (THERMAL_KEY.equals(key)) {
		strvalue = (String) newValue;
		if (DEBUG) Log.d(TAG, "Thermal setting changed: " + strvalue);
		setTHERMAL(strvalue);
		return true;
        } else if (ENABLE_HAL3_KEY.equals(key)) {
			value = (Boolean) newValue;
			mEnableHAL3.setChecked(value);
			setEnableHAL3(value);
			return true;
	} else {

    	value = (Boolean) newValue;
    	((SwitchPreference)preference).setChecked(value);
    	setEnable(key,value);
	return true;
	}
    }
}
