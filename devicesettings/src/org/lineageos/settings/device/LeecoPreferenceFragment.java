/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.device;

import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

public class LeecoPreferenceFragment extends PreferenceFragment {
    private static final String TAG = "LeecoPreferenceFragment";
    private static final String KEY_CAMERA_FOCUS_FIX_ENABLE = "camera_focus_enable";
    private static final String KEY_QUICK_CHARGE_ENABLE = "quick_charge_enable";

    private SwitchPreference mCameraFocusFixEnable;
    private SwitchPreference mQuickChargeEnable;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Log.d(TAG, "onCreatePreferences+++");
        addPreferencesFromResource(R.xml.leeco_settings_panel);
        final PreferenceScreen prefSet = getPreferenceScreen();
        mCameraFocusFixEnable = (SwitchPreference) findPreference(KEY_CAMERA_FOCUS_FIX_ENABLE);
        mQuickChargeEnable = (SwitchPreference) findPreference(KEY_QUICK_CHARGE_ENABLE);

        if (SettingsUtils.supportsCameraFocusFix()) {
            mCameraFocusFixEnable.setChecked(SettingsUtils.getCameraFocusFixEnabled(getActivity()));
            mCameraFocusFixEnable.setOnPreferenceChangeListener(mPrefListener);
            Log.d(TAG, "onCreatePreferences: cam focus fix enable = "
                    + SettingsUtils.getCameraFocusFixEnabled(getActivity()));
        } else {
            prefSet.removePreference(mCameraFocusFixEnable);
        }

        if (SettingsUtils.supportsQuickChargeSwitch()) {
            mQuickChargeEnable.setChecked(SettingsUtils.getQuickChargeEnabled(getActivity()));
            mQuickChargeEnable.setOnPreferenceChangeListener(mPrefListener);
            Log.d(TAG, "onCreatePreferences: quick charge enable = "
                    + SettingsUtils.getQuickChargeEnabled(getActivity()));
        } else {
            prefSet.removePreference(mQuickChargeEnable);
        }
        Log.d(TAG, "onCreatePreferences---");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume+++");
        getListView().setPadding(0, 0, 0, 0);
    }

    private Preference.OnPreferenceChangeListener mPrefListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                final String key = preference.getKey();

                if (KEY_CAMERA_FOCUS_FIX_ENABLE.equals(key)) {
                    boolean enabled = (boolean) value;
                    SettingsUtils.setCameraFocusFixEnabled(getActivity(), enabled);
                    SettingsUtils.writeCameraFocusFixSysfs(enabled);
                    Log.d(TAG, "onPreferenceChange: cam focus fix enable = " + enabled);
                } else if (KEY_QUICK_CHARGE_ENABLE.equals(key)) {
                    boolean enabled = (boolean) value;
                    SettingsUtils.setQuickChargeEnabled(getActivity(), enabled);
                    SettingsUtils.writeQuickChargeProp(enabled);
                    Log.d(TAG, "onPreferenceChange: quick charge enable = " + enabled);
                }

                return true;
            }
        };
}
