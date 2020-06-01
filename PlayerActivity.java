package com.vltima.ventsys.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.vltima.ventsys.R;
import com.vltima.ventsys.adapter.ServerAdapter;
import com.vltima.ventsys.adapter.SubtitleListAdapter;
import com.vltima.ventsys.model.Subtitle;
import com.vltima.ventsys.model.Video;
import com.vltima.ventsys.utils.ToastMsg;

import java.util.ArrayList;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static android.view.View.VISIBLE;
import static java.lang.String.valueOf;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";
    private static final String CLASS_NAME = "com.oxoo.spagreen.ui.activity.PlayerActivity";
    private PlayerView exoPlayerView;
    private ExoPlayer player;
    private RelativeLayout rootLayout;
    private MediaSource mediaSource;
    private boolean isPlaying;
    private List<Video> videos = new ArrayList<>();
    private Video video = null;
    private String url = "";
    private String videoType = "";
    private String category = "";
    private int visible;
    private ImageButton serverButton, subtitleButton;
    private LinearLayout rewindLayout, ffLayout, seekBarLayout;
    private TextView liveTvTextInController;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        url = getIntent().getStringExtra("streamUrl");
        videoType = getIntent().getStringExtra("videoType");
        category = getIntent().getStringExtra("category");
        video = (Video) getIntent().getSerializableExtra("video");
        intiViews();

        initVideoPlayer(url, videoType);

    }

    private void intiViews() {
        progressBar = findViewById(R.id.progress_bar);
        exoPlayerView = findViewById(R.id.player_view);
        rootLayout = findViewById(R.id.root_layout);
        serverButton = findViewById(R.id.img_server);
        subtitleButton = findViewById(R.id.img_subtitle);
        rewindLayout = findViewById(R.id.rewind_layout);
        ffLayout = findViewById(R.id.forward_layout);
        liveTvTextInController = findViewById(R.id.live_tv);
        seekBarLayout = findViewById(R.id.seekbar_layout);
        if (category.equalsIgnoreCase("tv")) {
            rewindLayout.setVisibility(View.GONE);
            ffLayout.setVisibility(View.GONE);
            serverButton.setVisibility(View.GONE);
            subtitleButton.setVisibility(View.GONE);
            seekBarLayout.setVisibility(View.GONE);
            liveTvTextInController.setVisibility(View.VISIBLE);
        }

        if (category.equalsIgnoreCase("tvseries")) {
            serverButton.setVisibility(View.GONE);
            //hide subtitle button if there is no subtitle
            if (video.getSubtitle().isEmpty()) {
                subtitleButton.setVisibility(View.GONE);
            }
        }

        if (category.equalsIgnoreCase("movie")) {
            ArrayList videoList = (ArrayList) getIntent().getSerializableExtra("videos");
            assert videoList != null;
            videos.addAll(videoList);
            //hide subtitle button if there is no subtitle
            if (video.getSubtitle().isEmpty()) {
                subtitleButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        subtitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //open subtitle dialog
                openSubtitleDialog();
            }
        });

        serverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open server dialog
                openServerDialog(videos);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!exoPlayerView.isControllerVisible()) {
            exoPlayerView.showController();
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!exoPlayerView.isControllerVisible()) {
                exoPlayerView.showController();
            }else {
                releasePlayer();
                finish();
            }
        } else if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            //Toast.makeText(this, "escape button", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void openServerDialog(List<Video> videos) {
        if (videos != null) {
            List<Video> videoList = new ArrayList<>();
            videoList.clear();

            for (Video video : videos) {
                if (!video.getFileType().equalsIgnoreCase("embed")) {
                    videoList.add(video);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
            View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_server_tv, null);
            RecyclerView serverRv = view.findViewById(R.id.serverRv);
            ServerAdapter serverAdapter = new ServerAdapter(PlayerActivity.this, videoList, "movie");
            serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
            serverRv.setHasFixedSize(true);
            serverRv.setAdapter(serverAdapter);

            Button closeBt = view.findViewById(R.id.close_bt);

            builder.setView(view);

            final AlertDialog dialog = builder.create();
            dialog.show();

            closeBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            final ServerAdapter.OriginalViewHolder[] viewHolder = {null};
            serverAdapter.setOnItemClickListener(new ServerAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, Video obj, int position, ServerAdapter.OriginalViewHolder holder) {
                    Intent playerIntent = new Intent(PlayerActivity.this, PlayerActivity.class);
                    //playerIntent.putExtra("id", id);
                    playerIntent.putExtra("videoType", obj.getFileType());
                    playerIntent.putExtra("streamUrl", obj.getFileUrl());
                    ArrayList<Video> videoListForIntent = new ArrayList<>(videoList);
                    playerIntent.putExtra("videos", videoListForIntent);
                    playerIntent.putExtra("video", obj); //including subtitle list
                    playerIntent.putExtra("category", "movie");
                    startActivity(playerIntent);
                    dialog.dismiss();
                    finish();
                }
            });
        } else {
            new ToastMsg(this).toastIconError(getString(R.string.no_other_server_found));
        }
    }

    private void openSubtitleDialog() {
        if (video != null) {
            if (!video.getSubtitle().isEmpty()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
                View view = LayoutInflater.from(PlayerActivity.this).inflate(R.layout.layout_subtitle_dialog, null);
                RecyclerView serverRv = view.findViewById(R.id.serverRv);
                SubtitleListAdapter adapter = new SubtitleListAdapter(PlayerActivity.this, video.getSubtitle());
                serverRv.setLayoutManager(new LinearLayoutManager(PlayerActivity.this));
                serverRv.setHasFixedSize(true);
                serverRv.setAdapter(adapter);

                Button closeBt = view.findViewById(R.id.close_bt);

                builder.setView(view);
                final AlertDialog dialog = builder.create();
                dialog.show();

                closeBt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                //click event
                adapter.setListener(new SubtitleListAdapter.OnSubtitleItemClickListener() {
                    @Override
                    public void onSubtitleItemClick(View view, Subtitle subtitle, int position, SubtitleListAdapter.SubtitleViewHolder holder) {
                        setSelectedSubtitle(mediaSource, subtitle.getUrl());
                        dialog.dismiss();
                    }
                });

            } else {
                new ToastMsg(this).toastIconError(getResources().getString(R.string.no_subtitle_found));
            }
        } else {
            new ToastMsg(this).toastIconError(getResources().getString(R.string.no_subtitle_found));
        }
    }

    private void setSelectedSubtitle(MediaSource mediaSource, String url) {
        MergingMediaSource mergedSource;
        if (url != null) {
            Uri subtitleUri = Uri.parse(url);

            Format subtitleFormat = Format.createTextSampleFormat(
                    null, // An identifier for the track. May be null.
                    MimeTypes.TEXT_VTT, // The mime type. Must be set correctly.
                    Format.NO_VALUE, // Selection flags for the track.
                    "en"); // The subtitle language. May be null.

            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(PlayerActivity.this,
                    Util.getUserAgent(PlayerActivity.this, CLASS_NAME), new DefaultBandwidthMeter());


            MediaSource subtitleSource = new SingleSampleMediaSource
                    .Factory(dataSourceFactory)
                    .createMediaSource(subtitleUri, subtitleFormat, C.TIME_UNSET);


            mergedSource = new MergingMediaSource(mediaSource, subtitleSource);
            player.prepare(mergedSource, false, false);
            player.setPlayWhenReady(true);
            //resumePlayer();

        } else {
            Toast.makeText(PlayerActivity.this, "there is no subtitle", Toast.LENGTH_SHORT).show();
        }
    }

    public void initVideoPlayer(String url, String type) {
        if (player != null) {
            player.release();

        }

        progressBar.setVisibility(VISIBLE);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new
                AdaptiveTrackSelection.Factory(bandwidthMeter);


        TrackSelector trackSelector = new
                DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance((Context) PlayerActivity.this, trackSelector);
        player.setPlayWhenReady(true);
        exoPlayerView.setPlayer(player);

        Uri uri = Uri.parse(url);

        if (type.equals("hls")) {
            mediaSource = hlsMediaSource(uri, PlayerActivity.this);
        } else if (type.equals("youtube")) {
            extractYoutubeUrl(url, PlayerActivity.this, 18);
        } else if (type.equals("youtube-live")) {
            Log.e("youtube url  :: ", url);
            extractYoutubeUrl(url, PlayerActivity.this, 133);
        } else if (type.equals("rtmp")) {
            mediaSource = rtmpMediaSource(uri);

        } else if (type.equals("mp3")) {
            mediaSource = mp3MediaSource(uri);
            RenderersFactory renderersFactory = new DefaultRenderersFactory(getApplicationContext());
            LoadControl loadControl = new DefaultLoadControl();
            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector);
            player.prepare(mediaSource);

        } else {
            mediaSource = mediaSource(uri, PlayerActivity.this);
        }

        player.prepare(mediaSource, true, false);

        player.addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    isPlaying = true;
                    progressBar.setVisibility(View.GONE);
                } else if (playbackState == Player.STATE_READY) {
                    isPlaying = false;
                    progressBar.setVisibility(View.GONE);
                } else if (playbackState == Player.STATE_BUFFERING) {
                    isPlaying = false;
                    progressBar.setVisibility(VISIBLE);
                    player.setPlayWhenReady(true);
                } else {
                    // player paused in any state
                    isPlaying = false;
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        exoPlayerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                visible = visibility;
            }
        });
    }


    private MediaSource mp3MediaSource(Uri uri) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), "ExoplayerDemo");
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        Handler mainHandler = new Handler();
        return new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, null);
    }

    private MediaSource mediaSource(Uri uri, Context context) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer")).
                createMediaSource(uri);
    }

    private MediaSource rtmpMediaSource(Uri uri) {
        MediaSource videoSource = null;

        RtmpDataSourceFactory dataSourceFactory = new RtmpDataSourceFactory();
        videoSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        return videoSource;
    }

    @SuppressLint("StaticFieldLeak")
    private void extractYoutubeUrl(String url, final Context context, final int tag) {

        new YouTubeExtractor(context) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles != null) {
                    int itag = tag;
                    String downloadUrl = ytFiles.get(itag).getUrl();
                    player.setPlayWhenReady(false);

                    try {

                        MediaSource mediaSource = mediaSource(Uri.parse(downloadUrl), context);
                        player.prepare(mediaSource, true, false);
                        player.setPlayWhenReady(true);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            }
        }.extract(url, true, true);


    }


    private MediaSource hlsMediaSource(Uri uri, Context context) {

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "oxoo"), bandwidthMeter);

        MediaSource videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        return videoSource;

    }

    @Override
    public void onBackPressed() {
        if (visible == View.GONE) {
            exoPlayerView.showController();
        } else {
            releasePlayer();
            super.onBackPressed();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.setPlayWhenReady(false);
            player.stop();
            player.release();
            player = null;
            exoPlayerView.setPlayer(null);
        }
    }



}
