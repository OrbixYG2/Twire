package com.perflyst.twire.fragments;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.transition.Transition;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.media.session.MediaButtonReceiver;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import com.balysv.materialripple.MaterialRippleLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.perflyst.twire.R;
import com.perflyst.twire.TwireApplication;
import com.perflyst.twire.activities.ChannelActivity;
import com.perflyst.twire.activities.stream.StreamActivity;
import com.perflyst.twire.adapters.PanelAdapter;
import com.perflyst.twire.chat.ChatManager;
import com.perflyst.twire.lowlatency.LLHlsPlaylistParserFactory;
import com.perflyst.twire.misc.FollowHandler;
import com.perflyst.twire.misc.OnlineSince;
import com.perflyst.twire.misc.ResizeHeightAnimation;
import com.perflyst.twire.misc.ResizeWidthAnimation;
import com.perflyst.twire.misc.Utils;
import com.perflyst.twire.model.ChannelInfo;
import com.perflyst.twire.model.Quality;
import com.perflyst.twire.model.SleepTimer;
import com.perflyst.twire.model.UserInfo;
import com.perflyst.twire.service.DialogService;
import com.perflyst.twire.service.Service;
import com.perflyst.twire.service.Settings;
import com.perflyst.twire.tasks.GetLiveStreamURL;
import com.perflyst.twire.tasks.GetPanelsTask;
import com.perflyst.twire.tasks.GetStreamChattersTask;
import com.perflyst.twire.tasks.GetStreamViewersTask;
import com.perflyst.twire.tasks.GetVODStreamURL;
import com.rey.material.widget.ProgressView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StreamFragment extends Fragment implements Player.Listener {
    private static int totalVerticalInset;
    private static boolean pipDisabling; // Tracks the PIP disabling animation.
    private final String LOG_TAG = getClass().getSimpleName();
    private final Handler fetchViewCountHandler = new Handler(),
            fetchChattersHandler = new Handler(),
            vodHandler = new Handler();
    private final HashMap<String, TextView> QualityOptions = new HashMap<>();
    private final int fetchViewCountDelay = 1000 * 60, // A minute
            fetchChattersDelay = 1000 * 60; // 30 seco... Nah just kidding. Also a minute.
    public StreamFragmentListener streamFragmentCallback;
    public boolean chatOnlyViewVisible = false;
    private boolean castingViewVisible = false,
            audioViewVisible = false,
            autoPlay = true,
            hasPaused = false,
            landscapeChatVisible = false;
    private UserInfo mUserInfo;
    private String vodId;
    private long startTime;
    private HeadsetPlugIntentReceiver headsetIntentReceiver;
    private Settings settings;
    private SleepTimer sleepTimer;
    private Map<String, Quality> qualityURLs;
    private boolean isLandscape = false;
    private Runnable fetchViewCountRunnable;
    private StyledPlayerView mVideoView;
    private ExoPlayer player;
    private MediaItem currentMediaItem;
    private Toolbar mToolbar;
    private ConstraintLayout mVideoInterface;
    private RelativeLayout mControlToolbar;
    private ConstraintLayout mVideoWrapper;
    private ConstraintLayout mPlayPauseWrapper;
    private ImageView mPauseIcon,
            mPlayIcon,
            mQualityButton,
            mFullScreenButton,
            mPreview,
            mShowChatButton;
    private TextView castingTextView, mCurrentViewersView, mRuntime;
    private AppCompatActivity mActivity;
    private Snackbar snackbar;
    private ProgressView mBufferingView;
    private BottomSheetDialog mQualityBottomSheet, mProfileBottomSheet;
    private CheckedTextView mAudioOnlySelector, mMuteSelector, mChatOnlySelector;
    private ViewGroup rootView;
    private MenuItem optionsMenuItem;
    private LinearLayout mQualityWrapper;
    private View mOverlay;

    private final Runnable vodRunnable = new Runnable() {
        @Override
        public void run() {
            if (player == null) return;

            ChatManager.updateVodProgress(player.getCurrentPosition(), false);

            if (player.isPlaying()) vodHandler.postDelayed(this, 1000);
        }
    };

    private int originalCtrlToolbarPadding;
    private int originalMainToolbarPadding;
    private int videoHeightBeforeChatOnly;
    private Integer triesForNextBest = 0;
    private boolean pictureInPictureEnabled; // Tracks if PIP is enabled including the animation.
    private MediaSessionCompat mediaSession;

    private static final DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
            .setUserAgent("Twire")
            .setDefaultRequestProperties(new HashMap<>() {{
                put("Referer", "https://player.twitch.tv");
                put("Origin", "https://player.twitch.tv");
            }});

    private static final MediaSource.Factory mediaSourceFactory = new HlsMediaSource.Factory(dataSourceFactory)
            .setPlaylistParserFactory(new LLHlsPlaylistParserFactory());

    public static StreamFragment newInstance(Bundle args) {
        StreamFragment fragment = new StreamFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Gets a Rect representing the usable area of the screen
     *
     * @return A Rect representing the usable area of the screen
     */
    public static Rect getScreenRect(Activity activity) {
        if (activity != null) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            Point size = new Point();
            int width, height;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode() && !activity.isInPictureInPictureMode() && !pipDisabling) {
                    display.getMetrics(metrics);
                } else {
                    display.getRealMetrics(metrics);
                }

                width = metrics.widthPixels;
                height = metrics.heightPixels;
            } else {
                display.getSize(size);
                width = size.x;
                height = size.y;
            }

            return new Rect(0, 0, Math.min(width, height), Math.max(width, height) - totalVerticalInset);
        }

        return new Rect();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        setHasOptionsMenu(true);
        settings = new Settings(getActivity());

        if (args != null) {
            mUserInfo = args.getParcelable(getString(R.string.stream_fragment_streamerInfo));
            vodId = args.getString(getString(R.string.stream_fragment_vod_id));
            autoPlay = args.getBoolean(getString(R.string.stream_fragment_autoplay));
        }

        final View mRootView = inflater.inflate(R.layout.fragment_stream, container, false);
        mRootView.requestLayout();

        // If the user has been in FULL SCREEN mode and presses the back button, we want to change the orientation to portrait.
        // As soon as the orientation has change we don't want to force the user to will be in portrait, so we "release" the request.
        if (requireActivity().getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true;
        }

        //  If no streamer info is available we cant show the stream.
        if (mUserInfo == null) {
            if (getActivity() != null) {
                getActivity().finish();
            }
            return rootView;
        }

        rootView = (ViewGroup) mRootView;
        mVideoInterface = mRootView.findViewById(R.id.video_interface);
        mToolbar = mRootView.findViewById(R.id.main_toolbar);
        mControlToolbar = mRootView.findViewById(R.id.control_toolbar_wrapper);
        mVideoWrapper = mRootView.findViewById(R.id.video_wrapper);
        mVideoView = mRootView.findViewById(R.id.VideoView);
        mPlayPauseWrapper = mRootView.findViewById(R.id.play_pause_wrapper);
        mPlayIcon = mRootView.findViewById(R.id.ic_play);
        mPauseIcon = mRootView.findViewById(R.id.ic_pause);
        mPreview = mRootView.findViewById(R.id.preview);
        mQualityButton = mRootView.findViewById(R.id.settings_icon);
        mFullScreenButton = mRootView.findViewById(R.id.fullscreen_icon);
        mShowChatButton = mRootView.findViewById(R.id.show_chat_button);
        castingTextView = mRootView.findViewById(R.id.chromecast_text);
        mBufferingView = mRootView.findViewById(R.id.exo_buffering);
        mCurrentViewersView = mRootView.findViewById(R.id.txtViewViewers);
        mRuntime = mRootView.findViewById(R.id.txtViewRuntime);
        mActivity = (AppCompatActivity) getActivity();
        mOverlay = mVideoView.getOverlayFrameLayout();

        landscapeChatVisible = settings.isChatInLandscapeEnabled();

        setupToolbar();
        setupSpinner();
        setupProfileBottomSheet();
        setupLandscapeChat();
        setupShowChatButton();

        if (savedInstanceState == null)
            setPreviewAndCheckForSharedTransition();

        mFullScreenButton.setOnClickListener(v -> toggleFullscreen());
        mPlayPauseWrapper.setOnClickListener(v -> player.setPlayWhenReady(!player.getPlayWhenReady()));

        initializePlayer();

        StyledPlayerControlView controlView = mVideoView.findViewById(R.id.exo_controller);

        controlView.setShowFastForwardButton(vodId != null);
        controlView.setShowRewindButton(vodId != null);

        controlView.setShowTimeoutMs(3000);
        controlView.setAnimationEnabled(false);

        controlView.addVisibilityListener(visibility -> {
            if (visibility == View.VISIBLE) {
                showVideoInterface();
            } else {
                hideVideoInterface();
            }
        });

        // Use ExoPlayer's overlay frame to intercept click events and pass them along
        mOverlay.setOnClickListener(view -> mVideoView.performClick());

        if (vodId == null) {
            View mTimeController = mRootView.findViewById(R.id.time_controller);
            mTimeController.setVisibility(View.INVISIBLE);

            if (!settings.getStreamPlayerRuntime()) {
                mRuntime.setVisibility(View.GONE);
            } else {
                startTime = args.getLong(getString(R.string.stream_fragment_start_time));
                controlView.setProgressUpdateListener((position, bufferedPosition) -> mRuntime.setText(OnlineSince.getOnlineSince(startTime)));
            }


            if (args != null && args.containsKey(getString(R.string.stream_fragment_viewers)) && settings.getStreamPlayerShowViewerCount()) {
                Utils.setNumber(mCurrentViewersView, args.getInt(getString(R.string.stream_fragment_viewers)));
                startFetchingViewers();
            } else {
                mCurrentViewersView.setVisibility(View.GONE);
            }
        } else {
            mCurrentViewersView.setVisibility(View.GONE);
            mRuntime.setVisibility(View.GONE);

            mRootView.findViewById(R.id.exo_position).setOnClickListener(v -> showSeekDialog());
        }

        keepScreenOn();

        if (autoPlay || vodId != null) {
            startStreamWithQuality(settings.getPrefStreamQuality());
        }

        headsetIntentReceiver = new HeadsetPlugIntentReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            requireActivity().registerReceiver(headsetIntentReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mRootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    DisplayCutout displayCutout = getDisplayCutout();
                    if (displayCutout != null) {
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            totalVerticalInset = displayCutout.getSafeInsetLeft() + displayCutout.getSafeInsetRight();
                        } else {
                            totalVerticalInset = displayCutout.getSafeInsetTop() + displayCutout.getSafeInsetBottom();
                        }

                        setVideoViewLayout();
                        setupLandscapeChat();
                        streamFragmentCallback.refreshLayout();
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            });
        }

        return mRootView;
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private DisplayCutout getDisplayCutout() {
        Activity activity = getActivity();
        if (activity != null) {
            WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (windowInsets != null) {
                return windowInsets.getDisplayCutout();
            }
        }

        return null;
    }

    private void initializePlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(getContext(), mediaSourceFactory)
                    .setSeekBackIncrementMs(10000)
                    .setSeekForwardIncrementMs(10000)
                    .build();
            player.addListener(this);
            mVideoView.setPlayer(player);

            if (vodId != null) {
                player.setPlaybackSpeed(settings.getPlaybackSpeed());
                player.setSkipSilenceEnabled(settings.getSkipSilence());
                player.setSeekParameters(SeekParameters.CLOSEST_SYNC);
                player.seekTo(settings.getVodProgress(vodId) * 1000L);
            }

            if (currentMediaItem != null) {
                player.setMediaItem(currentMediaItem, false);
                player.prepare();
            }

            PendingIntent pendingIntent = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                pendingIntent = PendingIntent.getBroadcast(
                        getContext(),
                        0, mediaButtonIntent,
                        PendingIntent.FLAG_IMMUTABLE
                );
            }

            ComponentName mediaButtonReceiver = new ComponentName(
                    getContext(), MediaButtonReceiver.class);
            mediaSession = new MediaSessionCompat(
                    getContext(),
                    getContext().getPackageName(),
                    mediaButtonReceiver,
                    pendingIntent);
            MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
            mediaSessionConnector.setPlayer(player);
            mediaSession.setActive(true);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            mediaSession.release();

            if (vodId != null) {
                settings.setVodProgress(vodId, player);
            }

            player.release();
            player = null;
        }
    }

    /* Player.Listener implementation */
    @Override
    public void onPlayerError(@NonNull PlaybackException exception) {
        Log.e(LOG_TAG, "Something went wrong playing the stream for " + mUserInfo.getDisplayName() + " - Exception: " + exception);

        playbackFailed();
    }

    @Override
    public void onPlayWhenReadyChanged(boolean isPlaying, int _ignored) {
        if (isPlaying) {
            showPauseIcon();
            keepScreenOn();

            if (!isAudioOnlyModeEnabled() && vodId == null) {
                player.seekToDefaultPosition(); // Go forward to live
            }
        } else {
            showPlayIcon();
            releaseScreenOn();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSurfaceSizeChanged(int width, int height) {
        Rect videoRect = new Rect();
        mVideoView.getVideoSurfaceView().getGlobalVisibleRect(videoRect);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            videoRect.left += totalVerticalInset;
            videoRect.right += totalVerticalInset;
        }

        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .setSourceRectHint(videoRect);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true);
        }

        mActivity.setPictureInPictureParams(builder.build());
    }

    @Override
    public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
        if (vodId == null || reason != Player.DISCONTINUITY_REASON_SEEK) return;

        boolean seekBackwards = oldPosition.positionMs > newPosition.positionMs;
        ChatManager.updateVodProgress(newPosition.positionMs, seekBackwards);
        if (seekBackwards) streamFragmentCallback.onSeek();
    }

    @Override
    public void onRenderedFirstFrame() {
        mPreview.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        if (isPlaying) vodRunnable.run();
    }

    public void backPressed() {
        mVideoView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        mFullScreenButton.setImageResource(isLandscape ? R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);

        checkShowChatButtonVisibility();
        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        pipDisabling = false;

        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
        }

        originalMainToolbarPadding = mToolbar.getPaddingRight();
        originalCtrlToolbarPadding = mControlToolbar.getPaddingRight();

        if (audioViewVisible && !isAudioOnlyModeEnabled()) {
            disableAudioOnlyView();
            startStreamWithQuality(settings.getPrefStreamQuality());
        } else if (!castingViewVisible && !audioViewVisible && hasPaused && settings.getStreamPlayerAutoContinuePlaybackOnReturn()) {
            startStreamWithQuality(settings.getPrefStreamQuality());
        }

        registerAudioOnlyDelegate();

        if (!chatOnlyViewVisible) {
            showVideoInterface();
            updateUI();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(LOG_TAG, "Stream Fragment paused");
        if (pictureInPictureEnabled)
            return;

        hasPaused = true;

        if (mQualityBottomSheet != null)
            mQualityBottomSheet.dismiss();

        if (mProfileBottomSheet != null)
            mProfileBottomSheet.dismiss();

        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }

        ChatManager.setPreviousProgress();
    }

    @Override
    public void onStop() {
        Log.d(LOG_TAG, "Stream Fragment Stopped");
        super.onStop();

        if (!castingViewVisible && !audioViewVisible) {
            player.pause();
        }

        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() != null) {
            getActivity().unregisterReceiver(headsetIntentReceiver);
        }
        Log.d(LOG_TAG, "Destroying");
        if (fetchViewCountRunnable != null) {
            fetchViewCountHandler.removeCallbacks(fetchViewCountRunnable);
        }

        super.onDestroy();
    }

    private void startFetchingCurrentChatters() {
        Runnable fetchChattersRunnable = new Runnable() {
            @Override
            public void run() {
                GetStreamChattersTask task = new GetStreamChattersTask(
                        new GetStreamChattersTask.GetStreamChattersTaskDelegate() {
                            @Override
                            public void onChattersFetched(ArrayList<String> chatters) {

                            }

                            @Override
                            public void onChattersFetchFailed() {

                            }
                        }, mUserInfo.getLogin()
                );

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                if (!StreamFragment.this.isDetached()) {
                    fetchChattersHandler.postDelayed(this, fetchChattersDelay);
                }
            }
        };

        fetchChattersHandler.post(fetchChattersRunnable);
    }

    /**
     * Starts fetching current viewers for the current stream
     */
    private void startFetchingViewers() {
        fetchViewCountRunnable = new Runnable() {
            @Override
            public void run() {
                GetStreamViewersTask task = new GetStreamViewersTask(
                        currentViewers -> {
                            try {
                                Log.d(LOG_TAG, "Fetching viewers");

                                Utils.setNumber(mCurrentViewersView, currentViewers);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }, mUserInfo.getUserId(), getContext()
                );

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                if (!StreamFragment.this.isDetached()) {
                    fetchViewCountHandler.postDelayed(this, fetchViewCountDelay);
                }
            }
        };


        fetchViewCountHandler.post(fetchViewCountRunnable);
    }

    /**
     * Sets up the show chat button.
     * Sets the correct visibility and the onclicklistener
     */
    private void setupShowChatButton() {
        checkShowChatButtonVisibility();
        mShowChatButton.setOnClickListener(view -> setLandscapeChat(!landscapeChatVisible));
    }

    /**
     * Sets the correct visibility of the show chat button.
     * If the screen is in landscape it is show, else it is shown
     */
    private void checkShowChatButtonVisibility() {
        if (isLandscape && settings.isChatInLandscapeEnabled()) {
            mShowChatButton.setVisibility(View.VISIBLE);
        } else {
            mShowChatButton.setVisibility(View.GONE);
        }
    }

    private void shareButtonClicked() {
        // https://stackoverflow.com/questions/17167701/how-to-activate-share-button-in-android-app
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody;

        if (vodId == null) {
            shareBody = "https://twitch.tv/" + mUserInfo.getLogin();
        } else {
            shareBody = "https://www.twitch.tv/" + mUserInfo.getLogin() + "/video/" + vodId;
        }

        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    private void profileButtonClicked() {
        mProfileBottomSheet.show();
    }

    private void sleepButtonClicked() {
        if (sleepTimer == null) {
            sleepTimer = new SleepTimer(new SleepTimer.SleepTimerDelegate() {
                @Override
                public void onTimesUp() {
                    stopAudioOnly();
                    player.pause();
                }

                @Override
                public void onStart(String message) {
                    showSnackbar(message);
                }

                @Override
                public void onStop(String message) {
                    showSnackbar(message);
                }
            }, getContext());
        }

        sleepTimer.show(getActivity());
    }

    private void playbackButtonClicked() {
        DialogService.getPlaybackDialog(getActivity(), player).show();
    }

    private void showSeekDialog() {
        DialogService.getSeekDialog(getActivity(), player).show();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        optionsMenuItem = menu.findItem(R.id.menu_item_options);
        optionsMenuItem.setVisible(false);
        optionsMenuItem.setOnMenuItemClickListener(menuItem -> {
            if (mQualityButton != null) {
                mQualityButton.performClick();
            }
            return true;
        });

        menu.findItem(R.id.menu_item_playback).setVisible(vodId != null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (!isVideoInterfaceShowing()) {
            mVideoWrapper.performClick();
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_item_sleep) {
            sleepButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_share) {
            shareButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_profile) {
            profileButtonClicked();
            return true;
        } else if (itemId == R.id.menu_item_external) {
            playWithExternalPlayer();
            return true;
        } else if (itemId == R.id.menu_item_playback) {
            playbackButtonClicked();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupLandscapeChat() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !settings.isChatLandscapeSwipeable() || !settings.isChatInLandscapeEnabled()) {
            return;
        }

        final int width = getScreenRect(getActivity()).height();

        View.OnTouchListener touchListener = new View.OnTouchListener() {
            private int downPosition = width;
            private int widthOnDown = width;

            public boolean onTouch(View view, MotionEvent event) {
                if (!isLandscape) {
                    return false;
                }

                final int X = (int) event.getRawX();
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        // If the user taps while the wrapper is in the resizing animation, cancel it.
                        mVideoWrapper.clearAnimation();

                        ConstraintLayout.LayoutParams lParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                        if (lParams.width > 0)
                            widthOnDown = lParams.width;

                        downPosition = (int) event.getRawX();
                        break;
                    case MotionEvent.ACTION_UP:
                        int upPosition = (int) event.getRawX();
                        int deltaPosition = upPosition - downPosition;

                        if (Math.abs(deltaPosition) < 20) {
                            setLandscapeChat(landscapeChatVisible);
                            return false;
                        }

                        setLandscapeChat(upPosition < downPosition);

                        break;
                    case MotionEvent.ACTION_MOVE:
                        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
                        int newWidth;

                        if (X > downPosition) { // Swiping right
                            newWidth = widthOnDown + X - downPosition;
                        } else { // Swiping left
                            newWidth = widthOnDown - (downPosition - X);
                        }

                        layoutParams.width = Math.max(Math.min(newWidth, width), width - getLandscapeChatTargetWidth());

                        mVideoWrapper.setLayoutParams(layoutParams);
                        break;

                }
                rootView.invalidate();
                return false;
            }
        };

        mOverlay.setOnTouchListener(touchListener);
    }

    private void setLandscapeChat(boolean visible) {
        landscapeChatVisible = visible;

        int width = getScreenRect(getActivity()).height();
        ResizeWidthAnimation resizeWidthAnimation = new ResizeWidthAnimation(mVideoWrapper, visible ? width - getLandscapeChatTargetWidth() : width);
        resizeWidthAnimation.setDuration(250);
        mVideoWrapper.startAnimation(resizeWidthAnimation);
        mShowChatButton.animate().rotation(visible ? 180f : 0).start();
    }

    private int getLandscapeChatTargetWidth() {
        return (int) (getScreenRect(getActivity()).height() * (settings.getChatLandscapeWidth() / 100.0));
    }

    private void initCastingView() {
        castingViewVisible = true;
        //auto.setVisibility(View.GONE); // Auto does not work on chromecast
        mVideoView.setVisibility(View.INVISIBLE);
        mBufferingView.setVisibility(View.GONE);
        castingTextView.setVisibility(View.VISIBLE);
        //castingTextView.setText(R.string.stream_chromecast_connecting);
        showVideoInterface();
    }

    private void disableCastingView() {
        castingViewVisible = false;
        //auto.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.VISIBLE);
        Service.bringToBack(mPreview);
        mBufferingView.setVisibility(View.VISIBLE);
        castingTextView.setVisibility(View.INVISIBLE);
        showVideoInterface();
    }

    /**
     * Checks if the activity was started with a shared view in high API levels.
     */
    private void setPreviewAndCheckForSharedTransition() {
        final Intent intent = requireActivity().getIntent();
        if (intent.hasExtra(getString(R.string.stream_preview_url))) {
            String imageUrl = intent.getStringExtra(getString(R.string.stream_preview_url));

            if (imageUrl == null || imageUrl.isEmpty()) {
                return;
            }

            Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUrl)
                    .signature(new ObjectKey(System.currentTimeMillis() / TimeUnit.MINUTES.toMillis(5))) // Refresh preview images every 5 minutes
                    .into(mPreview);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && intent.getBooleanExtra(getString(R.string.stream_shared_transition), false)) {
            mPreview.setTransitionName(getString(R.string.stream_preview_transition));

            final View[] viewsToHide = {mVideoView, mToolbar, mControlToolbar};
            for (View view : viewsToHide) {
                view.setVisibility(View.INVISIBLE);
            }

            getActivity().getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {

                @Override
                public void onTransitionEnd(Transition transition) {
                    TransitionManager.beginDelayedTransition(
                            mVideoWrapper,
                            new Fade()
                                    .setDuration(340)
                                    .excludeTarget(mVideoView, true)
                                    .excludeTarget(mPreview, true)
                    );

                    for (View view : viewsToHide) {
                        view.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    onTransitionEnd(transition);
                }

                public void onTransitionStart(Transition transition) {
                }

                public void onTransitionPause(Transition transition) {
                }

                public void onTransitionResume(Transition transition) {
                }
            });
        }

    }

    /**
     * Call to make sure the UI is shown correctly
     */
    private void updateUI() {
        setAndroidUiMode();
        keepControlIconsInView();
        setVideoViewLayout();
    }

    /**
     * Sets the System UI visibility so that the status- and navigation bar automatically hides if the app is current in landscape.
     * But they will automatically show when the user swipes the screen.
     */
    private void setAndroidUiMode() {
        if (getActivity() == null) {
            return;
        }

        Window window = getActivity().getWindow();
        View decorView = window.getDecorView();
        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(decorView);
        if (windowInsetsController == null) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(window, !isLandscape);

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        if (isLandscape) windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
        else windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
    }

    private void setVideoViewLayout() {
        ViewGroup.LayoutParams layoutParams = rootView.getLayoutParams();
        layoutParams.height = isLandscape ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;

        ConstraintLayout.LayoutParams layoutWrapper = (ConstraintLayout.LayoutParams) mVideoWrapper.getLayoutParams();
        if (isLandscape && !pictureInPictureEnabled) {
            layoutWrapper.width = !landscapeChatVisible ? ConstraintLayout.LayoutParams.MATCH_CONSTRAINT : getScreenRect(getActivity()).height() - getLandscapeChatTargetWidth();
            layoutWrapper.height = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
        } else {
            layoutWrapper.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;
            layoutWrapper.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
        }
        mVideoWrapper.setLayoutParams(layoutWrapper);

        mVideoView.setResizeMode(isLandscape ? AspectRatioFrameLayout.RESIZE_MODE_FIT : AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH);
    }

    /**
     * Checks if the video interface is fully showing
     */
    public boolean isVideoInterfaceShowing() {
        return mVideoInterface.getAlpha() == 1f;
    }

    /**
     * Hides the video control interface with animations
     */
    private void hideVideoInterface() {
        if (mToolbar != null && !audioViewVisible && !chatOnlyViewVisible) {
            mVideoInterface.animate().alpha(0f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        }
    }

    /**
     * Shows the video control interface with animations
     */
    private void showVideoInterface() {
        int MaintoolbarY = 0, CtrlToolbarY = 0;
        if (isLandscape && isDeviceBelowKitkat()) {
            MaintoolbarY = getStatusBarHeight();
        }
        if (isLandscape && Service.isTablet(getContext()) && isDeviceBelowKitkat()) {
            CtrlToolbarY = getNavigationBarHeight();
        }

        mControlToolbar.setTranslationY(-CtrlToolbarY);
        mToolbar.setTranslationY(MaintoolbarY);
        mVideoInterface.animate().alpha(1f).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    /**
     * Keeps the rightmost icons on the toolbars in view when the device is in landscape.
     * Otherwise the icons would be covered my the navigationbar
     */
    private void keepControlIconsInView() {
        if (isDeviceBelowKitkat() || settings.getStreamPlayerShowNavigationBar()) {
            int ctrlPadding = originalCtrlToolbarPadding;
            int mainPadding = originalMainToolbarPadding;
            int delta = getNavigationBarHeight();

            if (isLandscape && !Service.isTablet(getContext())) {
                ctrlPadding += delta;
                mainPadding += delta;
            }

            mShowChatButton.setPadding(0, 0, ctrlPadding, 0);
            mToolbar.setPadding(0, 0, mainPadding, 0);
            mControlToolbar.setPadding(0, 0, ctrlPadding, 0);
        }
    }

    /**
     * Returns the height of the statusbar
     */
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Returns the height of the navigation bar.
     * If the device doesn't have a navigation bar (Such as Samsung Galaxy devices) the height is 0
     */
    private int getNavigationBarHeight() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * If the device isn't currently in fullscreen a request is sent to turn the device into landscape.
     * Otherwise if the device is in fullscreen then is releases the lock by requesting for portrait
     */
    public void toggleFullscreen() {
        requireActivity().setRequestedOrientation(isLandscape ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    /**
     * Tries playing stream with a quality.
     * If the given quality doesn't exist for the stream the try the next best quality option.
     * If no Quality URLS have yet been created then try to start stream with an aync task.
     */
    private void startStreamWithQuality(String quality) {
        if (qualityURLs == null) {
            startStreamWithTask();
        } else {
            if (qualityURLs.containsKey(quality)) {
                if (chatOnlyViewVisible) {
                    return;
                }

                playUrl(qualityURLs.get(quality).URL);
                showQualities();
                updateSelectedQuality(quality);
                showPauseIcon();
                Log.d(LOG_TAG, "Starting Stream With a quality on " + quality + " for " + mUserInfo.getDisplayName());
                Log.d(LOG_TAG, "URLS: " + qualityURLs.keySet());
            } else if (!qualityURLs.isEmpty()) {
                Log.d(LOG_TAG, "Quality unavailable for this stream -  " + quality + ". Trying next best");
                tryNextBestQuality(quality);
            }
        }
    }

    /**
     * Starts an Async task that fetches all available Stream URLs for a stream,
     * then tries to start stream with the latest user defined quality setting.
     * If no URLs are available for the stream, the user is notified.
     */
    private void startStreamWithTask() {
        Consumer<Map<String, Quality>> callback = url -> {
            try {
                if (!url.isEmpty()) {
                    updateQualitySelections(url);
                    qualityURLs = url;

                    if (!isAudioOnlyModeEnabled()) {
                        startStreamWithQuality(new Settings(getContext()).getPrefStreamQuality());
                    }
                } else {
                    playbackFailed();
                }
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
            }
        };

        String[] types = getResources().getStringArray(R.array.PlayerType);

        if (vodId == null) {
            GetLiveStreamURL task = new GetLiveStreamURL(callback);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUserInfo.getLogin(), types[settings.getStreamPlayerType()]);
        } else {
            GetLiveStreamURL task = new GetVODStreamURL(callback);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vodId, types[settings.getStreamPlayerType()]);
        }
    }

    /**
     * Connects to the Twitch API to fetch the live stream url and quality selection urls.
     * If the task is successful the quality selector views' click listeners will be updated.
     */
    private void updateQualitySelectorsWithTask() {
        Consumer<Map<String, Quality>> delegate = url -> {
            try {
                if (!url.isEmpty()) {
                    updateQualitySelections(url);
                    qualityURLs = url;
                }
            } catch (IllegalStateException | NullPointerException e) {
                e.printStackTrace();
            }
        };

        if (vodId == null) {
            GetLiveStreamURL task = new GetLiveStreamURL(delegate);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mUserInfo.getLogin());
        } else {
            GetLiveStreamURL task = new GetVODStreamURL(delegate);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vodId.substring(1));
        }
    }

    /**
     * Stops the buffering and notifies the user that the stream could not be played
     */
    private void playbackFailed() {
        showSnackbar(getString(vodId == null ? R.string.stream_playback_failed : R.string.vod_playback_failed), getString(R.string.retry), v -> startStreamWithTask());
    }

    private void showSnackbar(String message) {
        showSnackbar(message, null, null);
    }

    private void showSnackbar(String message, String actionText, View.OnClickListener action) {
        if (getActivity() != null && !isDetached()) {
            View mainView = ((StreamActivity) getActivity()).getMainContentLayout();

            if ((snackbar == null || !snackbar.isShown()) && mainView != null) {
                snackbar = Snackbar.make(mainView, message, 4000);
                if (actionText != null)
                    snackbar.setAction(actionText, action);
                snackbar.show();
            }
        }

    }

    private void tryNextBestQuality(String quality) {
        if (triesForNextBest < qualityURLs.size() - 1) { // Subtract 1 as we don't count AUDIO ONLY as a quality
            triesForNextBest++;
            List<String> qualityList = new ArrayList<>(qualityURLs.keySet());
            int next = qualityList.indexOf(quality) + 1;
            if (next >= qualityList.size() - 1) {
                startStreamWithQuality(GetLiveStreamURL.QUALITY_SOURCE);
            } else {
                startStreamWithQuality(qualityList.get(next));
            }
        } else {
            playbackFailed();
        }
    }

    /**
     * Sets the URL to the VideoView and ChromeCast and starts playback.
     */
    private void playUrl(String url) {
        MediaItem mediaItem = new MediaItem.Builder()
                .setLiveConfiguration(new MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(1000).build())
                .setUri(url)
                .build();

        currentMediaItem = mediaItem;
        player.setMediaItem(mediaItem, false);
        player.prepare();

        player.play();
    }

    private void playWithExternalPlayer() {
        if (currentMediaItem.localConfiguration == null) {
            Toast.makeText(getContext(), R.string.error_external_playback_failed, Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(currentMediaItem.localConfiguration.uri, "video/*");
        startActivity(Intent.createChooser(intent, getString(R.string.stream_external_play_using)));
    }

    private void registerAudioOnlyDelegate() {
    }

    /**
     * Notifies the system that the screen though not timeout and fade to black.
     */
    private void keepScreenOn() {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Notifies the system that the screen can now time out.
     */
    private void releaseScreenOn() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updateSelectedQuality(String quality) {
        //TODO: Bad design
        if (quality == null) {
            resetQualityViewBackground(null);
        } else {
            resetQualityViewBackground(QualityOptions.get(quality));
        }
    }

    /**
     * Resets the background color of all the select quality views in the bottom dialog
     */
    private void resetQualityViewBackground(TextView selected) {
        for (TextView v : QualityOptions.values()) {
            if (v.equals(selected)) {
                v.setBackgroundColor(Service.getColorAttribute(R.attr.navigationDrawerHighlighted, R.color.grey_300, getContext()));
            } else {
                v.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparent));
            }
        }
    }

    /**
     * Adds the available qualities for a stream to the spinner menu
     */
    private void updateQualitySelections(Map<String, Quality> availableQualities) {
        for (TextView view : QualityOptions.values()) {
            mQualityWrapper.removeView((MaterialRippleLayout) view.getParent());
        }

        for (Map.Entry<String, Quality> entry : availableQualities.entrySet()) {
            Quality quality = entry.getValue();
            String qualityKey = entry.getKey();
            if (qualityKey.equals("audio_only"))
                continue;

            MaterialRippleLayout layout = (MaterialRippleLayout) LayoutInflater.from(getContext()).inflate(R.layout.quality_item, null);
            TextView textView = (TextView) layout.getChildAt(0);
            textView.setText(quality.Name.equals("Auto") ? getString(R.string.quality_auto) : quality.Name);

            setQualityOnClick(textView, qualityKey);
            QualityOptions.put(qualityKey, textView);
            mQualityWrapper.addView(layout);
        }
    }

    /**
     * Sets an OnClickListener on a select quality view (From bottom dialog).
     * The Listener starts the stream with a new quality setting and updates the background for the select quality views in the bottom dialog
     */
    private void setQualityOnClick(final TextView qualityView, String quality) {
        qualityView.setOnClickListener(v -> {
            // don´t set audio only mode as default
            if (!quality.equals("audio_only")) {
                settings.setPrefStreamQuality(quality);
            }
            // don`t allow to change the Quality when using audio only Mode
            if (!isAudioOnlyModeEnabled()) {
                startStreamWithQuality(quality);
                resetQualityViewBackground(qualityView);
                mQualityBottomSheet.dismiss();
            }
        });
    }

    private BottomSheetBehavior<View> getDefaultBottomSheetBehaviour(View bottomSheetView) {
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) bottomSheetView.getParent());
        behavior.setPeekHeight(requireActivity().getResources().getDisplayMetrics().heightPixels / 3);
        return behavior;
    }

    private void setupProfileBottomSheet() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.stream_profile_preview, null);
        mProfileBottomSheet = new BottomSheetDialog(requireContext());
        mProfileBottomSheet.setContentView(v);
        final BottomSheetBehavior<View> behavior = getDefaultBottomSheetBehaviour(v);

        mProfileBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        TextView mNameView = mProfileBottomSheet.findViewById(R.id.twitch_name);
        TextView mFollowers = mProfileBottomSheet.findViewById(R.id.txt_followers);
        TextView mViewers = mProfileBottomSheet.findViewById(R.id.txt_viewers);
        ImageView mFollowButton = mProfileBottomSheet.findViewById(R.id.follow_unfollow_icon);
        ImageView mFullProfileButton = mProfileBottomSheet.findViewById(R.id.full_profile_icon);
        RecyclerView mPanelsRecyclerView = mProfileBottomSheet.findViewById(R.id.panel_recyclerview);
        if (mNameView == null || mFollowers == null || mViewers == null || mFullProfileButton == null || mPanelsRecyclerView == null)
            return;

        mNameView.setText(mUserInfo.getDisplayName());

        TwireApplication.backgroundPoster.post(() -> {
            ChannelInfo channelInfo = Service.getStreamerInfoFromUserId(mUserInfo.getUserId(), getContext());
            TwireApplication.uiThreadPoster.post(() -> {
                channelInfo.getFollowers(getContext(), followers -> Utils.setNumber(mFollowers, followers), 0);
                Utils.setNumber(mViewers, channelInfo.getViews());

                setupFollowButton(mFollowButton, channelInfo);
            });
        });

        mFullProfileButton.setOnClickListener(view -> {
            mProfileBottomSheet.dismiss();

            final Intent intent = new Intent(getContext(), ChannelActivity.class);
            intent.putExtra(getString(R.string.channel_info_intent_object), mUserInfo);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        });

        setupPanels(mPanelsRecyclerView);
    }

    private void setupFollowButton(final ImageView imageView, ChannelInfo channelInfo) {
        final FollowHandler mFollowHandler = new FollowHandler(
                channelInfo,
                getContext(),
                () -> imageView.setVisibility(View.GONE)
        );
        updateFollowIcon(imageView, mFollowHandler.isStreamerFollowed());

        imageView.setOnClickListener(view -> {
            final boolean isFollowed = mFollowHandler.isStreamerFollowed();
            if (isFollowed) {
                mFollowHandler.unfollowStreamer();
            } else {
                mFollowHandler.followStreamer();
            }

            final int ANIMATION_DURATION = 240;

            imageView.animate()
                    .setDuration(ANIMATION_DURATION)
                    .alpha(0f)
                    .start();

            new Handler().postDelayed(() -> {
                updateFollowIcon(imageView, !isFollowed);
                imageView.animate().alpha(1f).setDuration(ANIMATION_DURATION).start();
            }, ANIMATION_DURATION);
        });
    }

    private void updateFollowIcon(ImageView imageView, boolean isFollowing) {
        @DrawableRes int imageRes = isFollowing
                ? R.drawable.ic_heart_broken
                : R.drawable.ic_favorite;
        imageView.setImageResource(imageRes);
    }

    private void setupPanels(RecyclerView recyclerView) {
        final PanelAdapter mPanelAdapter = new PanelAdapter(getActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(mPanelAdapter);

        GetPanelsTask mTask = new GetPanelsTask(mUserInfo.getLogin(), mPanelAdapter::addPanels);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Setups the Quality Select spinner.
     * Automatically hides the text of the selected Quality
     */
    private void setupSpinner() {
        mQualityButton.setOnClickListener(v -> mQualityBottomSheet.show());

        View v = LayoutInflater.from(getContext()).inflate(R.layout.stream_settings, null);
        mQualityBottomSheet = new BottomSheetDialog(requireContext());
        mQualityBottomSheet.setContentView(v);

        final BottomSheetBehavior behavior = getDefaultBottomSheetBehaviour(v);

        mQualityBottomSheet.setOnDismissListener(dialogInterface -> behavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        mQualityWrapper = mQualityBottomSheet.findViewById(R.id.quality_wrapper);
        mAudioOnlySelector = mQualityBottomSheet.findViewById(R.id.audio_only_selector);
        mMuteSelector = mQualityBottomSheet.findViewById(R.id.mute_selector);
        mChatOnlySelector = mQualityBottomSheet.findViewById(R.id.chat_only_selector);
        TextView optionsTitle = mQualityBottomSheet.findViewById(R.id.options_text);

        if (optionsTitle != null) {
            optionsTitle.setVisibility(View.VISIBLE);
        }

        if (vodId == null) {
            mChatOnlySelector.setVisibility(View.VISIBLE);
        }

        mAudioOnlySelector.setVisibility(View.VISIBLE);
        mAudioOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            audioOnlyClicked();
        });

        mMuteSelector.setVisibility(View.VISIBLE);
        mMuteSelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            muteClicked();
        });

        mChatOnlySelector.setOnClickListener(view -> {
            mQualityBottomSheet.dismiss();
            chatOnlyClicked();
        });
    }

    private void initAudioOnlyView() {
        if (!audioViewVisible) {
            audioViewVisible = true;
            mVideoView.setVisibility(View.INVISIBLE);
            mBufferingView.start();
            //mBufferingView.setVisibility(View.GONE);
            castingTextView.setVisibility(View.VISIBLE);
            castingTextView.setText(R.string.stream_audio_only_active);

            showVideoInterface();
            updateSelectedQuality(null);
            startStreamWithQuality("audio_only");
            hideQualities();
        }
    }

    private void disableAudioOnlyView() {
        if (audioViewVisible) {
            audioViewVisible = false;
            mAudioOnlySelector.setChecked(false);
            mVideoView.setVisibility(View.VISIBLE);
            mBufferingView.setVisibility(View.VISIBLE);
            Service.bringToBack(mPreview);
            castingTextView.setVisibility(View.INVISIBLE);

            showQualities();
            showVideoInterface();
        }
    }

    private boolean isAudioOnlyModeEnabled() {
        // just use audioViewVisible as boolean
        return audioViewVisible;
    }

    private void audioOnlyClicked() {
        mAudioOnlySelector.setChecked(!mAudioOnlySelector.isChecked());
        if (mAudioOnlySelector.isChecked()) {
            initAudioOnlyView();
        } else {
            stopAudioOnly();
        }
    }

    private void muteClicked() {
        mMuteSelector.setChecked(!mMuteSelector.isChecked());
        if (mMuteSelector.isChecked()) {
            player.setVolume(0);
        } else {
            player.setVolume(1);
        }
    }

    private void stopAudioOnly() {
        disableAudioOnlyView();

        // start stream with last quality
        releasePlayer();
        initializePlayer();
        updateSelectedQuality(settings.getPrefStreamQuality());
        startStreamWithQuality(settings.getPrefStreamQuality());

        // resume the stream
        player.play();
    }

    private void stopAudioOnlyNoServiceCall() {
        disableAudioOnlyView();
    }

    private void chatOnlyClicked() {
        mChatOnlySelector.setChecked(!mChatOnlySelector.isChecked());
        if (mChatOnlySelector.isChecked()) {
            initChatOnlyView();
        } else {
            disableChatOnlyView();
        }
    }

    private void initChatOnlyView() {
        if (!chatOnlyViewVisible) {
            chatOnlyViewVisible = true;
            if (isLandscape) {
                toggleFullscreen();
            }

            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            videoHeightBeforeChatOnly = mVideoWrapper.getHeight();
            ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, (int) getResources().getDimension(R.dimen.main_toolbar_height));
            heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            heightAnimation.setDuration(240);
            mVideoWrapper.startAnimation(heightAnimation);

            mPlayPauseWrapper.setVisibility(View.GONE);
            mControlToolbar.setVisibility(View.GONE);
            mToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.colorPrimary, R.color.primary, requireContext()));

            releasePlayer();
            optionsMenuItem.setVisible(true);

            showVideoInterface();
            updateSelectedQuality(null);
            hideQualities();
        }
    }

    private void disableChatOnlyView() {
        if (chatOnlyViewVisible) {
            chatOnlyViewVisible = false;
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            ResizeHeightAnimation heightAnimation = new ResizeHeightAnimation(mVideoWrapper, videoHeightBeforeChatOnly);
            heightAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
            heightAnimation.setDuration(240);
            heightAnimation.setFillAfter(false);
            mVideoWrapper.startAnimation(heightAnimation);

            mControlToolbar.setVisibility(View.VISIBLE);
            mPlayPauseWrapper.setVisibility(View.VISIBLE);
            mToolbar.setBackgroundColor(Service.getColorAttribute(R.attr.streamToolbarColor, R.color.black_transparent, requireActivity()));

            if (!castingViewVisible) {
                initializePlayer();
                startStreamWithQuality(settings.getPrefStreamQuality());
            }

            optionsMenuItem.setVisible(false);

            showVideoInterface();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean enabled) {
        mVideoInterface.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
        pictureInPictureEnabled = enabled;

        if (!enabled)
            pipDisabling = true;
    }

    /**
     * Setups the toolbar by giving it a bit of extra right padding (To make sure the icons are 16dp from right)
     * Also adds the main toolbar as the support actionbar
     */
    private void setupToolbar() {
        mToolbar.setPadding(0, 0, Service.dpToPixels(requireActivity(), 5), 0);
        setHasOptionsMenu(true);
        mActivity.setSupportActionBar(mToolbar);
        ActionBar actionBar = mActivity.getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(mUserInfo.getDisplayName());
        mActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //mActivity.getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        mToolbar.bringToFront();
    }

    /**
     * Rotates the Play Pause wrapper with an Rotation Animation.
     */
    private void rotatePlayPauseWrapper() {
        RotateAnimation rotate = new RotateAnimation(mPlayPauseWrapper.getRotation(),
                360, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        int PLAY_PAUSE_ANIMATION_DURATION = 500;
        rotate.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        mPlayPauseWrapper.startAnimation(rotate);
    }

    /**
     * Checks if the device is below SDK API 19 (Kitkat)
     *
     * @return the result
     */
    private boolean isDeviceBelowKitkat() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT;
    }

    private void showPauseIcon() {
        if (mPauseIcon.getAlpha() == 0f) {
            rotatePlayPauseWrapper();
            mPauseIcon.animate().alpha(1f).start();
            mPlayIcon.animate().alpha(0f).start();
        }
    }

    private void showPlayIcon() {
        if (mPauseIcon.getAlpha() != 0f) {
            rotatePlayPauseWrapper();
            mPauseIcon.animate().alpha(0f).start();
            mPlayIcon.animate().alpha(1f).start();
        }
    }

    private void showQualities() {
        mQualityWrapper.setVisibility(View.VISIBLE);
    }

    private void hideQualities() {
        mQualityWrapper.setVisibility(View.GONE);
    }

    public interface StreamFragmentListener {
        void onSeek();

        void refreshLayout();
    }

    /**
     * Broadcast class for detecting when the user plugs or unplug a headset.
     */
    private class HeadsetPlugIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        if (player.isPlaying()) {
                            Log.d(LOG_TAG, "Chat, pausing from headsetPlug");
                            showVideoInterface();
                            player.pause();
                        }
                        break;
                    case 1:
                        showVideoInterface();
                        break;
                    default:

                }
            }
        }
    }
}
