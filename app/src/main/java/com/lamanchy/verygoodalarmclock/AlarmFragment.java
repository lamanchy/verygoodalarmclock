package com.lamanchy.verygoodalarmclock;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class AlarmFragment extends Fragment {
    CustomPreferences preferences;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    LocalBroadcastManager localBroadcastManager;

    private Unbinder unbinder;
    @BindView(R.id.regular_alarm_time) TextView regularAlarmTime;
    @BindView(R.id.regular_alarm_toggle) ToggleButton regularAlarmToggle;
    @BindView(R.id.one_time_alarm_time) TextView oneTimeAlarmTime;
    @BindView(R.id.one_time_change_toggle) ToggleButton oneTimeChangeToggle;
    @BindView(R.id.one_time_off_toggle) ToggleButton oneTimeOffToggle;

    public static AlarmFragment newInstance(String prefix) {
        Bundle args = new Bundle();
        args.putString("prefix", prefix);
        AlarmFragment fragment = new AlarmFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        String prefix = getArguments().getString("prefix");
        preferences = new CustomPreferences(getContext(), prefix);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                setContents();
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(listener);
        setContents();
    }

    public void setContents() {
        regularAlarmTime.setText(preferences.getTimeAsString(Enums.REGULAR_ALARM));
        regularAlarmToggle.setChecked(preferences.getEnabled(Enums.REGULAR_ALARM));

        oneTimeAlarmTime.setText(preferences.getTimeAsString(Enums.ONE_TIME_ALARM));
        oneTimeChangeToggle.setChecked(preferences.getEnabled(Enums.ONE_TIME_ALARM));
        oneTimeOffToggle.setChecked(preferences.getEnabled(Enums.ONE_TIME_OFF));
    }

    @Override
    public void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public void runTimePicker(final String type) {
        Integer currentTime = preferences.getTime(type);

        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                preferences.setTime(type, hour * 60 + minute );
                if (!preferences.getEnabled(type)) {
                    preferences.flipEnabled(type);
                }
            }
        };
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), listener,
                currentTime / 60, currentTime % 60, true);

        timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                setContents();
            }
        });
        timePickerDialog.setTitle(R.string.time_picker_title);
        timePickerDialog.show();
    }

    @OnClick(R.id.regular_alarm_time)
    public void onAlarmTimeClick() {
        runTimePicker(Enums.REGULAR_ALARM);
    }

    @OnClick(R.id.one_time_alarm_time)
    public void onOneTimeAlarmTimeClick() {
        runTimePicker(Enums.ONE_TIME_ALARM);
    }

    public void commonFlipProcedure(String type) {
        if (!preferences.getEnabled(type)) {
            runTimePicker(type);
        } else {
            preferences.flipEnabled(type);
        }
    }

    @OnClick(R.id.regular_alarm_toggle)
    public void onRegularAlarmToggleClick() {
        // if turning off regular alarm, first turn off one time off
        // it cant be on when regular was off
        commonFlipProcedure(Enums.REGULAR_ALARM);
    }

    @OnClick(R.id.one_time_change_toggle)
    public void onOneTimeAlarmToggleClick() {
        // if turning off one time alarm, first turn off one time off
        // it cant be on when one time alarm was off
        commonFlipProcedure(Enums.ONE_TIME_ALARM);
    }

    @OnClick(R.id.one_time_off_toggle)
    public void onOneTimeOffToggleClick() {
        try {
            preferences.flipEnabled(Enums.ONE_TIME_OFF);
        } catch (IllegalStateException e) {
            Toast.makeText(
                    getContext(),
                    "No alarm to turn off (even once)",
                    Toast.LENGTH_LONG).show();
            setContents();
        }
    }
}
