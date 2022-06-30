package com.perflyst.twire.service;

import android.app.Activity;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.github.stephenvinouze.materialnumberpickercore.MaterialNumberPicker;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.perflyst.twire.R;
import com.perflyst.twire.views.LayoutSelector;
import com.rey.material.widget.CheckedTextView;
import com.rey.material.widget.Slider;

import java.util.Arrays;

/**
 * Created by Sebastian Rask on 02-05-2016.
 */
public class DialogService {
    public static MaterialDialog getThemeDialog(final Activity activity) {
        final String CURRENT_THEME = new Settings(activity).getTheme();
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(activity);
        final MaterialSimpleListItem blueTheme = getThemeDialogAdapterItem(R.string.blue_theme_name, R.drawable.circle_theme_blue_chooser, activity);
        final MaterialSimpleListItem purpleTheme = getThemeDialogAdapterItem(R.string.purple_theme_name, R.drawable.circle_theme_purple_chooser, activity);
        final MaterialSimpleListItem blackTheme = getThemeDialogAdapterItem(R.string.black_theme_name, R.drawable.circle_theme_black_chooser, activity);
        final MaterialSimpleListItem nightTheme = getThemeDialogAdapterItem(R.string.night_theme_name, R.drawable.circle_theme_night_chooser, activity);
        final MaterialSimpleListItem trueNightTheme = getThemeDialogAdapterItem(R.string.true_night_theme_name, R.drawable.circle_theme_black_chooser, activity);
        adapter.addAll(blueTheme, purpleTheme, blackTheme, nightTheme, trueNightTheme);

        final MaterialDialog.Builder dialog = getBaseThemedDialog(activity)
                .title(R.string.theme_dialog_title)
                .adapter(adapter, (dialog1, itemView, which, text) -> {
                    String theme = adapter.getItem(which).getContent().toString();

                    new Settings(activity).setTheme(theme);
                    if (!theme.equals(CURRENT_THEME)) {
                        activity.recreate();
                    }
                });

        return dialog.build();
    }

    private static MaterialSimpleListItem getThemeDialogAdapterItem(@StringRes int title, @DrawableRes int icon, Activity activity) {
        MaterialSimpleListItem.Builder builder = new MaterialSimpleListItem.Builder(activity)
                .content(title)
                .icon(icon);

        return builder.build();
    }

    public static MaterialDialog getSettingsLoginOrLogoutDialog(Activity activity, String username) {
        return getBaseThemedDialog(activity)
                .content(activity.getString(R.string.gen_dialog_login_or_out_content, username))
                .positiveText(R.string.gen_dialog_login_or_out_login_action)
                .negativeText(R.string.gen_dialog_login_or_out_logout_action).build();
    }

    public static MaterialDialog getSettingsWipeFollowsDialog(Activity activity) {
        return getBaseThemedDialog(activity)
                .content(R.string.gen_dialog_wipe_follows_content)
                .positiveText(R.string.gen_dialog_wipe_follows_action)
                .negativeText(R.string.cancel).build();
    }

    public static MaterialDialog getSettingsExportFollowsDialog(Activity activity) {
        return getBaseThemedDialog(activity)
                .content(R.string.gen_dialog_export_follows_content)
                .positiveText(R.string.gen_dialog_export_follows_action)
                .negativeText(R.string.cancel).build();
    }

    public static MaterialDialog getSettingsImportFollowsDialog(Activity activity) {
        return getBaseThemedDialog(activity)
                .content(R.string.gen_dialog_import_follows_content)
                .positiveText(R.string.gen_dialog_import_follows_action)
                .negativeText(R.string.cancel).build();
    }

    public static MaterialDialog getChooseStartUpPageDialog(Activity activity, String currentlySelectedPageTitle, MaterialDialog.ListCallbackSingleChoice listCallbackSingleChoice) {
        final Settings settings = new Settings(activity);
        @ArrayRes int arrayResource = settings.isLoggedIn() ? R.array.StartupPages : R.array.StartupPagesNoLogin;

        int indexOfPage = 0;
        String[] androidStrings = activity.getResources().getStringArray(arrayResource);
        for (int i = 0; i < androidStrings.length; i++) {
            if (androidStrings[i].equals(currentlySelectedPageTitle)) {
                indexOfPage = i;
                break;
            }
        }

        return getBaseThemedDialog(activity)
                .title(R.string.gen_start_page)
                .items(arrayResource)
                .itemsCallbackSingleChoice(indexOfPage, listCallbackSingleChoice)
                .positiveText(android.R.string.ok)
                .negativeText(R.string.cancel)
                .build();
    }

    public static MaterialDialog getChooseStreamCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_stream, R.array.StreamsCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceStreamStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.stream_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_streams_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseGameCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_game, R.array.GameCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceGameStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.game_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_game_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseStreamerCardStyleDialog(Activity activity, LayoutSelector.OnLayoutSelected onLayoutSelected) {
        LayoutSelector layoutSelector = new LayoutSelector(R.layout.cell_channel, R.array.FollowCardStyles, onLayoutSelected, activity)
                .setSelectedLayoutTitle(new Settings(activity).getAppearanceChannelStyle())
                .setTextColorAttr(R.attr.navigationDrawerTextColor)
                .setPreviewMaxHeightRes(R.dimen.subscription_card_preview_max_height);

        return getBaseThemedDialog(activity)
                .title(R.string.appearance_streamer_style_title)
                .customView(layoutSelector.build(), true)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseCardSizeDialog(Activity activity, @StringRes int dialogTitle, String currentlySelected, MaterialDialog.ListCallbackSingleChoice callbackSingleChoice) {
        int indexOfPage = 0;
        String[] sizeTitles = activity.getResources().getStringArray(R.array.CardSizes);
        for (int i = 0; i < sizeTitles.length; i++) {
            if (sizeTitles[i].equals(currentlySelected)) {
                indexOfPage = i;
                break;
            }
        }

        return getBaseThemedDialog(activity)
                .title(dialogTitle)
                .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
                .items(sizeTitles)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChooseChatSizeDialog(Activity activity, @StringRes int dialogTitle, @ArrayRes int array, int currentSize, MaterialDialog.ListCallbackSingleChoice callbackSingleChoice) {
        int indexOfPage = currentSize - 1;
        String[] sizeTitles = activity.getResources().getStringArray(array);

        return getBaseThemedDialog(activity)
                .title(dialogTitle)
                .itemsCallbackSingleChoice(indexOfPage, callbackSingleChoice)
                .items(sizeTitles)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getChoosePlayerTypeDialog(Activity activity, @StringRes int dialogTitle, @ArrayRes int array, int currentSize, MaterialDialog.ListCallbackSingleChoice callbackSingleChoice) {
        String[] playerTypes = activity.getResources().getStringArray(array);

        return getBaseThemedDialog(activity)
                .title(dialogTitle)
                .itemsCallbackSingleChoice(currentSize, callbackSingleChoice)
                .items(playerTypes)
                .positiveText(R.string.done)
                .build();
    }

    public static MaterialDialog getSleepTimerDialog(Activity activity, boolean isTimerRunning, MaterialDialog.SingleButtonCallback onStartCallback, MaterialDialog.SingleButtonCallback onStopCallBack, int hourValue, int minuteValue) {

        @StringRes int positiveText = isTimerRunning ? R.string.resume : R.string.start;
        @StringRes int negativeText = isTimerRunning ? R.string.stop : R.string.cancel;


        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(R.string.stream_sleep_timer_title)
                .customView(R.layout.dialog_sleep_timer, false)
                .positiveText(positiveText)
                .negativeText(negativeText)
                .onPositive(onStartCallback)
                .onNegative(onStopCallBack)
                .build();

        View customView = dialog.getCustomView();
        MaterialNumberPicker hourPicker = customView.findViewById(R.id.hourPicker);
        MaterialNumberPicker minPicker = customView.findViewById(R.id.minutePicker);

        hourPicker.setValue(hourValue);
        minPicker.setValue(minuteValue);

        return dialog;
    }

    private static long newTime = 0;
    public static MaterialDialog getSeekDialog(Activity activity, Player player) {
        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(R.string.stream_seek_dialog_title)
                .customView(R.layout.dialog_seek, false)
                .positiveText(R.string.done)
                .negativeText(R.string.cancel)
                .onPositive((dialog1, which) -> player.seekTo(newTime))
                .build();

        View customView = dialog.getCustomView();
        MaterialNumberPicker hourPicker = customView.findViewById(R.id.hour_picker);
        MaterialNumberPicker minutePicker = customView.findViewById(R.id.minute_picker);
        MaterialNumberPicker secondPicker = customView.findViewById(R.id.second_picker);

        int maxProgress = (int) (player.getDuration() / 1000);
        hourPicker.setMaxValue(maxProgress / 3600);
        minutePicker.setMaxValue(Math.min(maxProgress / 60, 59));
        secondPicker.setMaxValue(Math.min(maxProgress, 59));

        int currentProgress = (int) (player.getCurrentPosition() / 1000);
        hourPicker.setValue(currentProgress / 3600);
        minutePicker.setValue(currentProgress / 60 % 60);
        secondPicker.setValue(currentProgress % 60);

        NumberPicker.OnValueChangeListener updateTime = (a, b, c) -> newTime = (hourPicker.getValue() * 3600L + minutePicker.getValue() * 60L + secondPicker.getValue()) * 1000;
        hourPicker.setOnValueChangedListener(updateTime);
        minutePicker.setOnValueChangedListener(updateTime);
        secondPicker.setOnValueChangedListener(updateTime);

        newTime = player.getCurrentPosition();

        return dialog;
    }

    public static MaterialDialog getSliderDialog(Activity activity, MaterialDialog.SingleButtonCallback onCancelCallback, Slider.OnPositionChangeListener sliderChangeListener, int startValue, int minValue, int maxValue, String title) {
        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(title)
                .customView(R.layout.dialog_slider, false)
                .positiveText(R.string.done)
                .negativeText(R.string.cancel)
                .onNegative(onCancelCallback)
                .build();

        View customView = dialog.getCustomView();
        if (customView != null) {
            Slider slider = customView.findViewById(R.id.slider);
            slider.setValueRange(minValue, maxValue, false);
            slider.setValue(startValue, false);
            slider.setOnPositionChangeListener(sliderChangeListener);
        }

        return dialog;
    }

    public static MaterialDialog getPlaybackDialog(Activity activity, ExoPlayer player) {
        MaterialDialog dialog = getBaseThemedDialog(activity)
                .title(R.string.menu_playback)
                .customView(R.layout.dialog_playback, false)
                .positiveText(R.string.done)
                .build();

        View customView = dialog.getCustomView();
        Settings settings = new Settings(customView.getContext());

        // Speed
        Float initialSpeed = settings.getPlaybackSpeed();
        Float[] speedValues = new Float[] { 0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f };

        TextView speedDisplay = customView.findViewById(R.id.speed_display);
        Slider slider = customView.findViewById(R.id.speed_slider);
        slider.setValue(Arrays.asList(speedValues).indexOf(initialSpeed), false);
        slider.setValueDescriptionProvider((value) -> activity.getString(R.string.playback_speed, speedValues[value]));
        slider.setOnPositionChangeListener((view, fromUser, oldPos, newPos, oldValue, newValue) -> {
            float newSpeed = speedValues[newValue];
            settings.setPlaybackSpeed(newSpeed);
            player.setPlaybackSpeed(newSpeed);
            speedDisplay.setText(activity.getString(R.string.playback_speed_display, newSpeed));
        });

        speedDisplay.setText(activity.getString(R.string.playback_speed_display, initialSpeed));

        // Skip Silence
        CheckedTextView skipSilenceView = customView.findViewById(R.id.skip_silence);
        skipSilenceView.setChecked(settings.getSkipSilence());
        skipSilenceView.setOnClickListener((view) -> {
            boolean newState = !settings.getSkipSilence();
            settings.setSkipSilence(newState);
            player.setSkipSilenceEnabled(newState);
            skipSilenceView.setChecked(newState);
        });

        return dialog;
    }

    public static MaterialDialog getRouterErrorDialog(Activity activity, int errorMessage) {
        return getBaseThemedDialog(activity)
                .title(R.string.router_error_dialog_title)
                .content(errorMessage)
                .cancelListener(dialogInterface -> activity.finish())
                .build();
    }

    public static MaterialDialog.Builder getBaseThemedDialog(Activity activity) {
        return new MaterialDialog.Builder(activity)
                .titleColorAttr(R.attr.navigationDrawerTextColor)
                .backgroundColorAttr(R.attr.navigationDrawerBackground)
                .contentColorAttr(R.attr.navigationDrawerTextColor)
                .itemsColorAttr(R.attr.navigationDrawerTextColor);
    }
}
