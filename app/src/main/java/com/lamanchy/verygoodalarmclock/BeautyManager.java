package com.lamanchy.verygoodalarmclock;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class BeautyManager {
    private AlarmFragment alarmFragment;
    private CustomPreferences preferences;

    public BeautyManager(AlarmFragment alarmFragment, CustomPreferences preferences) {
        this.alarmFragment = alarmFragment;
        this.preferences = preferences;
    }

    public static float getWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels / displayMetrics.density;
    }

    public float getWidth() {
        return getWidth(Objects.requireNonNull(alarmFragment.getContext()));
    }

    public Collection<Animator> getAnimations() {
        List<Animator> animations = new ArrayList<>();
        animations.add(sizeAnimator(alarmFragment.regularAlarmTime));
        animations.add(sizeAnimator(alarmFragment.regularAlarmHint));
        animations.add(sizeAnimator(alarmFragment.regularAlarmMiddle));
        animations.add(sizeAnimator(alarmFragment.oneTimeAlarmTime));

        animations.add(centerAnimator(alarmFragment.regularAlarmPart));
        animations.add(centerAnimator(alarmFragment.oneTimeAlarmTime));

        animations.add(colorAnimator(alarmFragment.regularAlarmTime));
        animations.add(colorAnimator(alarmFragment.oneTimeAlarmTime));

        animations.add(timeAnimator(alarmFragment.regularAlarmTime));
        animations.add(timeAnimator(alarmFragment.oneTimeAlarmTime));

        return animations;
    }

    public Animator timeAnimator(TextView text) {
        return ObjectAnimator.ofInt(new Timerable(text), "Time", getTime(text));
    }

    public Animator colorAnimator(TextView text) {
        Animator animator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            animator = ObjectAnimator.ofArgb(new Colorable(text), "TextColor", getTextColor(text));
        } else {
            animator = ObjectAnimator.ofInt(new Colorable(text), "TextColor", getTextColor(text));
            animator.setDuration(0);
        }
        return animator;
    }

    public Animator centerAnimator(View view) {
        return ObjectAnimator.ofFloat(new Centerable(view), "CenterRatio", getCenterRatio(view));
    }

    public Animator sizeAnimator(TextView text) {
        return ObjectAnimator.ofFloat(new Sizeable(text), "TextSPSize", getTextSize(text));
    }

    public int getTime(TextView text) {
        return text == alarmFragment.regularAlarmTime
                ? preferences.getTime(Enums.REGULAR_ALARM)
                : preferences.getTime(Enums.ONE_TIME_ALARM);
    }

    public int getBigTextSize() {
        return getSmallTextSize() * 5;
    }

    public int getSmallTextSize() {
        if (getWidth() < 400) return 12;
        if (getWidth() < 600) return 14;
        return 16;
    }

    public float getTextSize(TextView text) {
        if (text == alarmFragment.oneTimeAlarmTime) {
            return preferences.getEnabled(Enums.ONE_TIME_ALARM) ? getBigTextSize() : getSmallTextSize();
        } else if (text == alarmFragment.regularAlarmTime) {
            return preferences.getEnabled(Enums.ONE_TIME_ALARM) ? getSmallTextSize() : getBigTextSize();
        } else {
            return (preferences.getEnabled(Enums.ONE_TIME_ALARM)
                    && preferences.getEnabled(Enums.REGULAR_ALARM)) ? getSmallTextSize() : 0;
        }
    }

    public float getCenterRatio(View view) {
        if (view == alarmFragment.regularAlarmPart) {
            return preferences.getEnabled(Enums.ONE_TIME_ALARM) ? 0 : 1;
        } else {
            return preferences.getEnabled(Enums.ONE_TIME_ALARM) ? 1 : 0;
        }
    }

    public int getEnabledColor() {
        return ResourcesCompat.getColor(alarmFragment.getResources(), R.color.colorPrimarySecondaryText, null);
    }

    public int getDisabledColor() {
        return ResourcesCompat.getColor(alarmFragment.getResources(), R.color.colorDisabledText, null);
    }

    public int getTextColor(TextView text) {
        if (text == alarmFragment.oneTimeAlarmTime) {
            return preferences.getEnabled(Enums.ONE_TIME_ALARM) ? getEnabledColor() : getDisabledColor();
        } else {
            return (preferences.getEnabled(Enums.REGULAR_ALARM)
                    && !preferences.getEnabled(Enums.ONE_TIME_OFF)) ? getEnabledColor() : getDisabledColor();
        }
    }

    public class Timerable {
        TextView view;

        public Timerable(TextView view) {
            this.view = view;
        }

        public int getTime() {
            String text = view.getText().toString();
            String[] parts = text.split(":");
            return Integer.valueOf(parts[0]) * 60 + Integer.valueOf(parts[1]);
        }

        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        public void setTime(int time) {
            view.setText(String.format("%02d", time / 60) + ':' + String.format("%02d", time % 60));
        }
    }

    public class Colorable {
        TextView view;

        public Colorable(TextView view) {
            this.view = view;
        }

        public int getTextColor() {
            return view.getCurrentTextColor();
        }

        public void setTextColor(int color) {
            view.setTextColor(color);
        }
    }

    public class Sizeable {
        TextView view;

        public Sizeable(TextView view) {
            this.view = view;
        }

        public float getTextSPSize() {
            return view.getTextSize() / alarmFragment.getResources().getDisplayMetrics().scaledDensity;
        }

        public void setTextSPSize(float size) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    public class Centerable {
        View view;

        public Centerable(View view) {
            this.view = view;
        }

        public float getCenterRatio() {
            return getRightPadding() / getCenteredRightPadding();
        }

        public void setCenterRatio(float centerRatio) {
            setRightPadding((int) (centerRatio * getCenteredRightPadding()));
        }

        public float getCenteredRightPadding() {
            return (getParentWidth() - view.getWidth()) / 2;

        }

        public Integer getParentWidth() {
            return ((View) view.getParent()).getWidth();
        }

        public Integer getRightPadding() {
            return view.getPaddingRight();
        }

        public void setRightPadding(Integer rightPadding) {
            view.setPadding(0, 0, rightPadding, 0);
        }
    }
}
