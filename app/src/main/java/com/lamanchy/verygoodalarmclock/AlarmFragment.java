package com.lamanchy.verygoodalarmclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class AlarmFragment extends Fragment {
    CustomPreferences preferences;
    SharedPreferences.OnSharedPreferenceChangeListener listener;
    AnimatorSet animator = new AnimatorSet();
    Long animationEnd = 0L;
    @BindView(R.id.regular_alarm_time)
    TextView regularAlarmTime;
    @BindView(R.id.regular_alarm_hint)
    TextView regularAlarmHint;
    @BindView(R.id.regular_alarm_middle)
    TextView regularAlarmMiddle;
    @BindView(R.id.regular_alarm_part)
    LinearLayout regularAlarmPart;
    @BindView(R.id.one_time_alarm_time)
    TextView oneTimeAlarmTime;
    @BindView(R.id.regular_alarm_toggle)
    ToggleButton regularAlarmToggle;
    @BindView(R.id.one_time_change_toggle)
    ToggleButton oneTimeChangeToggle;
    @BindView(R.id.one_time_off_toggle)
    ToggleButton oneTimeOffToggle;
    @BindView(R.id.alarm_title)
    TextView alarmTitle;
    @BindView(R.id.filler)
    TextView filler;
    @BindView(R.id.base)
    LinearLayout base;
    private Unbinder unbinder;
    private BeautyManager beautyManager;
    private SongManager songManager;
    private Toast toast = null;

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
        final String prefix = getArguments().getString("prefix");
        preferences = new CustomPreferences(getContext(), prefix);
        beautyManager = new BeautyManager(this, preferences);
        songManager = new SongManager(getContext());
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.startsWith(preferences.getPrefix())) {
                    setContents(true);
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);
        unbinder = ButterKnife.bind(this, view);

        // to run animation on view render
        setViewTreeObservers(view);
        return view;
    }

    public void setViewTreeObservers(View view) {
        setViewTreeObserver(view == null ? getView() : view);
    }

    public void setViewTreeObserver(final View view) {
        if (view != null) {
            ViewTreeObserver vto = view.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setContents(false);
                }
            });
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(listener);
        setContents(false);
    }


    public void setContents(Boolean animate) {
        if (animate) {
            animationEnd = System.currentTimeMillis() + 600;
            beautyManager.useRightPadding = true;
            makeToast();
        }

        alarmTitle.setText(preferences.getPrefix().equals(Enums.MORNIN_PREFIX) ? R.string.mornin_title : R.string.evenin_title);

        regularAlarmToggle.setChecked(preferences.getEnabled(Enums.REGULAR_ALARM));
        oneTimeOffToggle.setChecked(preferences.getEnabled(Enums.ONE_TIME_OFF));
        oneTimeChangeToggle.setChecked(preferences.getEnabled(Enums.ONE_TIME_ALARM));
        regularAlarmToggle.setTextSize(beautyManager.getSmallTextSize());
        oneTimeOffToggle.setTextSize(beautyManager.getSmallTextSize());
        oneTimeChangeToggle.setTextSize(beautyManager.getSmallTextSize());

        animator.removeAllListeners();
        animator.pause();
        animator = new AnimatorSet();
        animator.playTogether(beautyManager.getAnimations());
        animator.setDuration(Math.max(animationEnd - System.currentTimeMillis(), 0));
        animator.setInterpolator(new LinearInterpolator()); // so when animation is interrupted,
        // it continues the same speed
        animator.start();

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // I dont know why, but sometimes animation ends sooner that it should.
                // Unfortunatelly I dont have time to deal with it, so this sometimes
                // "glitches", but always ends in the right state. Or the animation could be
                // disabled, but I spent already too much time on it, just to disable it :D
                if (animation.getDuration() > 0) {
                    setContents(false);
                } else {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    setViewTreeObservers(null);
                }
            }
        });
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
                preferences.setTime(type, hour * 60 + minute);
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
                setContents(true);
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
        commonFlipProcedure(Enums.REGULAR_ALARM);
    }

    @OnClick(R.id.one_time_change_toggle)
    public void onOneTimeAlarmToggleClick() {
        commonFlipProcedure(Enums.ONE_TIME_ALARM);
    }

    @OnClick(R.id.one_time_off_toggle)
    public void onOneTimeOffToggleClick() {
        try {
            preferences.flipEnabled(Enums.ONE_TIME_OFF);
        } catch (IllegalStateException e) {
            Toast.makeText(
                    getContext(),
                    "No alarm to turn off (even once)",     // R.string....
                    Toast.LENGTH_LONG).show();
            setContents(true);
        }
    }

    private void makeToast() {

        Long timeToAlarmMillis = songManager.getNextAlarmTime(preferences, true);
        String text;
        if (timeToAlarmMillis == null) {
            text = getString(preferences.getPrefix().equals(Enums.MORNIN_PREFIX)
                    ? R.string.mornin_toast_no_alarm : R.string.evenin_toast_no_alarm);
        } else {
            timeToAlarmMillis = timeToAlarmMillis - System.currentTimeMillis();
            int minutes = (int) ((timeToAlarmMillis / (1000 * 60)) % 60);
            int hours = (int) (timeToAlarmMillis / (1000 * 60 * 60) % 24);
            int days = (int) (timeToAlarmMillis / (1000 * 60 * 60 * 24));
            List<Integer> twothreefour = Arrays.asList(2, 3, 4);
            text = getString(preferences.getPrefix().equals(Enums.MORNIN_PREFIX)
                    ? R.string.mornin_toast_alarm_in : R.string.evenin_toast_alarm_in) + " ";
            if (days == 1) { // is never more that one day
                text += getString(R.string.day) + ", ";
            }
            text += String.valueOf(hours) + " ";
            text += getString(hours == 1 ? R.string.hours1 : (twothreefour.contains(hours) ? R.string.hours234 : R.string.hoursmore));
            text += " " + getString(R.string.and) + " ";
            text += String.valueOf(minutes) + " ";
            text += getString(minutes == 1 ? R.string.minutes1 : (twothreefour.contains(minutes) ? R.string.minutes234 : R.string.minutesmore));

        }
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
