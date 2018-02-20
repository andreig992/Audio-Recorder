package andrei.audiorecorder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity {

    String reservedChars[] = {"|", "\\", "?", "*", "<", "\"", ":", ">"};
    NotificationManager mNotifyMgr;

    String saveFolder = "Audio Recorder";
    String savePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + saveFolder + "/";
    //^^ THE DEFAULT SAVE FOLDER
    LinearLayout parentView;
    Boolean useCompatPadding = true;
    Integer recCardRadius = 4;
    Integer controlsCardRadius = 0;
    Integer cardElevation = 4;
    Integer controlsCardElevation = 4;
    //^^ INITIALIZES THE DEFAULT STYLE OF THE RECORDING CARDS AND WHERE THEY'RE PLACED

    CardView settingsView;
    ScrollView settingsViewScroll;
    ImageView emptyImage;
    CardView controlsView;
    ImageButton deleteBtn;
    ImageButton recordPauseBtn;
    ImageButton saveBtn;
    Chronometer time;
    //^^ INITIALIZES RECORDING CONTROL BUTTONS

    EditText customRecordingName;
    TextView sampleRateTextView;
    TextView isSampleRateSupportedTextView;
    SeekBar sampleRateBar;
    TextView audioEncoderBitRateTextView;
    SeekBar audioEncoderBitRateBar;
    TextView audioQualityTextView;
    TextView audioFileSizePerMinuteTextView;

    Boolean isRecording;
    Boolean isPaused;
    //^^ INITIALIZES THE RECORDING STATES;

    String filename;
    String filenameTemp;
    long pauseTime;

    List<TextView> cardTitles = new ArrayList<>();
    List<File> myfile = new ArrayList<>();
    List<ImageView> imageViews = new ArrayList<>();
    List<LinearLayout> seekBars = new ArrayList<>();
    List<MediaPlayer> mediaPlayers = new ArrayList<>();
    List<Boolean> isClickedArr = new ArrayList<>();

    MediaRecorder mRecorder;
    String upordown;
    AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
    AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
    File recordingFile;
    File recordingFileTemp;
    File temp = new File(savePath + "Temp");
    String extension = ".mp3";
    Boolean hasMicPermission = false;
    Integer samplingRates[] = {8000, 11025, 16000, 22050, 32000, 44100, 48000};
    List<Boolean> samplingRatesSupported = new ArrayList<>();
    List<Boolean> bitRates = new ArrayList<>();
    Handler handler;
    Integer alreadySetAudioChannels = AudioFormat.CHANNEL_IN_DEFAULT;
    Integer alreadySetAudioSamplingRate = 16000;
    Integer alreadySetAudioEncoderBitRate = 512000;
    Integer alreadySetAudioEncoder = MediaRecorder.AudioEncoder.DEFAULT;
    Button audioEncoderBttn;
    Button audioMonoBttn;
    Button audioStereoBttn;
    Boolean isSetUp = false;
    LinearLayout linearlayout;

    public static boolean stringContainsItemFromList(String inputString, String[] items) {
        for (int i = 0; i < items.length; i++) {
            if (inputString.contains(items[i])) {
                return true;
            }
        }
        return false;
    }

    public static String getStringSizeLengthFile(long size) {

        DecimalFormat df = new DecimalFormat("0.00");

        float sizeKb = 1024.0f;
        float sizeMo = sizeKb * sizeKb;
        float sizeGo = sizeMo * sizeKb;
        float sizeTerra = sizeGo * sizeKb;


        if (size < sizeMo)
            return df.format(size / sizeKb) + " KB";
        else if (size < sizeGo)
            return df.format(size / sizeMo) + " MB";
        else if (size < sizeTerra)
            return df.format(size / sizeGo) + " GB";

        return "";
    }

    public String getTimeString(long duration) {
        int minutes = (int) Math.floor(duration / 1000 / 60);
        int seconds = (int) ((duration / 1000) - (minutes * 60));

        return minutes + ":" + String.format("%02d", seconds);
    }

    void loadRecordingsFromStorage() {

        new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                if (temp.exists()) {
                    try {
                        FileUtils.forceDelete(temp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                File f = new File(savePath);
                File[] file = f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.isDirectory();
                    }
                });
                if (file != null) {
                    if (file.length > 0) {
                        emptyImage.startAnimation(fadeOut);
                        emptyImage.setVisibility(View.GONE);
                    } else {
                        emptyImage.startAnimation(fadeIn);
                        emptyImage.setVisibility(View.VISIBLE);
                    }

                    Arrays.sort(file, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                        }
                    });
                    Integer i = 0;
                    while (i < file.length) {
                        myfile.add(i, file[i]);
                        createCard(file[i], i);
                        System.out.println("Created card with file: " + myfile.get(i).getAbsolutePath() + ", number " + (i + 1) + " out of " + file.length);
                        i++;
                    }
                }
            }
        }.run();
    }

    private void openSettingsView() {
        if (upordown == "down" && !isRecording) {
            //OPEN SETTINGS VIEW
            if (hasMicPermission) {
                settingsView.setVisibility(View.VISIBLE);
                if (myfile.size() == 0) {
                    emptyImage.startAnimation(fadeOut);
                    emptyImage.setVisibility(View.GONE);
                }
                upordown = "up";
                System.out.println("Controls view UP");
            }

        }
    }

    private void closeSettingsView() {
        if (upordown == "up") {
            //CLOSE SETTINGS VIEW
            settingsView.setVisibility(View.GONE);
            upordown = "down";
            if (myfile.size() == 0) {
                Runnable a = new Runnable() {
                    @Override
                    public void run() {
                        emptyImage.startAnimation(fadeIn);
                        emptyImage.setVisibility(View.VISIBLE);
                    }
                };
                handler.postDelayed(a, 500);
            }
            System.out.println("Controls view DOWN");
        }
    }

    void recheckQualityAndSizeData() {
        System.out.println("Rechecking variables...");
        System.out.println("Nothing...");
    }

    boolean getValidSamplingRates(Integer SampleRate, Integer Channels) {

        int bufferSize = AudioRecord.getMinBufferSize(SampleRate, Channels, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize > 0) {
            System.out.println(SampleRate + " SUPPORTED " + " + " + bufferSize);
            return true;
        } else {
            System.out.println(SampleRate + " NOT SUPPORTED " + " + " + bufferSize);
            return false;
        }
    }

    void setUpSettingsView() {



        settingsViewScroll.addView(getLayoutInflater().inflate(R.layout.settings_layout_1, null));
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Integer y = size.y;
        settingsViewScroll.getLayoutParams().height = (int) (y / 2.15);
        customRecordingName = (EditText) findViewById(R.id.customRecordingName);

        final DecimalFormat fmtSampleRates = new DecimalFormat("##,### Hz");
        final DecimalFormat fmtBitRates = new DecimalFormat("#,### kbps");

        customRecordingName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filename = customRecordingName.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        sampleRateTextView = (TextView) findViewById(R.id.sampleRate);
        isSampleRateSupportedTextView = (TextView) findViewById(R.id.isSampleRateSupported);
        isSampleRateSupportedTextView.setVisibility(View.GONE);

        sampleRateBar = (SeekBar) findViewById(R.id.sampleRateBar);

        audioQualityTextView = (TextView) findViewById(R.id.audioQualityText);
        audioFileSizePerMinuteTextView = (TextView) findViewById(R.id.audioFileSizePerMinute);
        audioEncoderBitRateTextView = (TextView) findViewById(R.id.audioEncodingBitRate);
        audioEncoderBitRateTextView.setText(fmtBitRates.format(alreadySetAudioEncoderBitRate / 1000));
        audioEncoderBitRateBar = (SeekBar) findViewById(R.id.audioEncodingBitRateBar);
        audioEncoderBitRateBar.setProgress(alreadySetAudioEncoderBitRate);


        for (Integer samplingRate : samplingRates) {
            samplingRatesSupported.add(getValidSamplingRates(samplingRate, alreadySetAudioChannels));
        }

        ArrayList<Integer> SamplSupp = new ArrayList<>();
        for (Integer sample : samplingRates) {
            if (samplingRatesSupported.get(Arrays.asList(samplingRates).indexOf(sample))) {
                SamplSupp.add(sample);
                //MUST BE AFTER FILLING UP samplingRatesSupported
            }
        }
        sampleRateTextView.setText(fmtSampleRates.format(SamplSupp.get(1)));
        sampleRateBar.setProgress(Arrays.asList(samplingRates).indexOf(SamplSupp.get(1)));
        alreadySetAudioSamplingRate = SamplSupp.get(1);

        sampleRateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {

                sampleRateTextView.setText(fmtSampleRates.format(samplingRates[progress]));
                if (!samplingRatesSupported.get(progress)) {
                    sampleRateTextView.setPaintFlags(sampleRateTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    isSampleRateSupportedTextView.setVisibility(View.VISIBLE);
                    System.out.println("Sample rate " + samplingRates[progress] + " NOT supported");

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(Arrays.asList(samplingRates).indexOf(alreadySetAudioSamplingRate));
                            sampleRateTextView.setPaintFlags(0);
                            isSampleRateSupportedTextView.setVisibility(View.GONE);
                        }
                    };
                    handler.postDelayed(runnable, 2500);

                } else if (samplingRatesSupported.get(progress)) {
                    alreadySetAudioSamplingRate = Arrays.asList(samplingRates).get(progress);
                    System.out.println("Successfully set audioSampling rate to " + samplingRates[progress]);
                    recheckQualityAndSizeData();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        audioEncoderBitRateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
                audioEncoderBitRateTextView.setText(fmtBitRates.format(progress / 1000));
                alreadySetAudioEncoderBitRate = progress;
                System.out.println("Successfully set BitRate rate to " + progress);
                recheckQualityAndSizeData();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        audioEncoderBttn = (Button) findViewById(R.id.encoderButton);
        audioMonoBttn = (Button) findViewById(R.id.monoButton);
        audioStereoBttn = (Button) findViewById(R.id.stereoButton);
        audioEncoderBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme)
                        .setTitle("Select your prefered Audio Encoder")
                        .setView(getLayoutInflater().inflate(R.layout.encoder_selector, null))
                        .setPositiveButton(R.string.ok, null)
                        .setNegativeButton(R.string.cancel, null)
                        .create()
                        .show();
            }
        });


        audioStereoBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioMonoBttn.setBackgroundResource(R.drawable.button_drawable);
                audioMonoBttn.setTextColor(getResources().getColor(R.color.settingsTextColor));
                audioStereoBttn.setBackgroundResource(R.drawable.button_drawable_selected);
                audioStereoBttn.setTextColor(getResources().getColor(R.color.colorControls));
                alreadySetAudioChannels = AudioFormat.CHANNEL_IN_STEREO;
                System.out.println("Audio set to STEREO");
            }
        });
        audioMonoBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioMonoBttn.setBackgroundResource(R.drawable.button_drawable_selected);
                audioMonoBttn.setTextColor(getResources().getColor(R.color.colorControls));
                audioStereoBttn.setBackgroundResource(R.drawable.button_drawable);
                audioStereoBttn.setTextColor(getResources().getColor(R.color.settingsTextColor));
                alreadySetAudioChannels = AudioFormat.CHANNEL_IN_MONO;
                System.out.println("Audio set to MONO");
            }
        });
        isSetUp = true;
    }

    void checkForStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme)
                    .setTitle(R.string.permissionNeeded)
                    .setMessage(R.string.thisAppRequiresAccessToThePhonesStorageInOrderToReadAndWriteAudioFiles)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    0);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            loadRecordingsFromStorage();
        }
    }

    void playRandomAnimation(View view) {
        Random random = new Random();
        int numberOfMethods = 12;

        switch (random.nextInt(numberOfMethods)) {
            case 0:
                YoYo.with(Techniques.Wobble).playOn(view);
                break;
            case 1:
                YoYo.with(Techniques.Wave).playOn(view);
                break;
            case 2:
                YoYo.with(Techniques.Swing).playOn(view);
                break;
            case 3:
                YoYo.with(Techniques.Landing).playOn(view);
                break;
            case 4:
                YoYo.with(Techniques.FlipInX).playOn(view);
                break;
            case 5:
                YoYo.with(Techniques.BounceInUp).playOn(view);
                break;
            case 6:
                YoYo.with(Techniques.BounceInDown).playOn(view);
                break;
            case 7:
                YoYo.with(Techniques.BounceIn).playOn(view);
                break;
            case 8:
                YoYo.with(Techniques.Shake).playOn(view);
                break;
            case 9:
                YoYo.with(Techniques.RubberBand).playOn(view);
                break;
            case 10:
                YoYo.with(Techniques.Bounce).playOn(view);
                break;
            case 11:
                YoYo.with(Techniques.Tada).playOn(view);
                break;
            case 12:
                YoYo.with(Techniques.DropOut).playOn(view);
                break;
        }
    }

    void checkForMicPermission(Boolean shouldStartRecording) {
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            playRandomAnimation(controlsView);
            new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme)
                    .setTitle(R.string.permissionNeeded)
                    .setMessage(R.string.accessToTheDevicesMicrophoneIsNeededToRecordAudio)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    1);
                            if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            }

                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            if (!isSetUp) {
                setUpSettingsView();
            }
            if (shouldStartRecording) {
                YoYo.with(Techniques.Bounce).playOn(linearlayout);
                startRecording();
            }
            hasMicPermission = true;
        }
    }

    Toolbar my_toolbar;
        @Override
        protected void onCreate( final Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
            my_toolbar.setTitle("");
            setSupportActionBar(my_toolbar);


            System.out.println("Application Started");

            mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //^^ INITIALIZES THE NOTIFICATION MANAGER
            handler = new Handler();

            parentView = (LinearLayout) findViewById(R.id.parentView);

            emptyImage = (ImageView) findViewById(R.id.emptyImage);
            fadeIn.setDuration(3000);
            fadeOut.setDuration(200);

            linearlayout = (LinearLayout) findViewById(R.id.linearLayout);

            controlsView = (CardView) findViewById(R.id.controlsView);
            controlsView.setCardElevation(controlsCardElevation);
            controlsView.setRadius(controlsCardRadius);
            upordown = "down";

            settingsView = (CardView) findViewById(R.id.settingsView);
            settingsViewScroll = (ScrollView) findViewById(R.id.settingsViewScroll);
            settingsView.setVisibility(View.GONE);
            final int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO);

            controlsView.setOnTouchListener(new OnSwipeTouchListener(this) {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        hasMicPermission = true;
                    }
                    if (!isSetUp) {
                        setUpSettingsView();
                    }

                    return super.onTouch(v, event);
                }

                @Override
                public void onClick() {
                    System.out.println("Clicked");
                    if (upordown == "down") {
                        checkForMicPermission(false);
                        openSettingsView();
                    } else {
                        closeSettingsView();
                    }
                    super.onClick();
                }

                @Override
                public void onSwipeTop() {
                    System.out.println("Top");
                    checkForMicPermission(false);
                    openSettingsView();
                    super.onSwipeTop();
                }

                @Override
                public void onSwipeBottom() {
                    System.out.println("Bottom");
                    closeSettingsView();
                    super.onSwipeBottom();
                }
            });

            time = (Chronometer) findViewById(R.id.time);
            isRecording = false;
            isPaused = true;
            deleteBtn = (ImageButton) findViewById(R.id.deleteButton);
            deleteBtn.setVisibility(View.INVISIBLE);
            recordPauseBtn = (ImageButton) findViewById(R.id.recordPauseBtn);
            saveBtn = (ImageButton) findViewById(R.id.saveButton);
            saveBtn.setVisibility(View.INVISIBLE);

            checkForStoragePermission();

            recordPauseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (!isRecording) {
                        checkForMicPermission(true);
                    } else {
                        if (isPaused) {
                            resumeRecording();
                        } else {
                            pauseRecording();
                        }
                    }
                }
            });
            recordPauseBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    System.out.println(cardTitles.size());
                    System.out.println(myfile.size());
                    System.out.println(imageViews.size());
                    System.out.println(seekBars.size());
                    System.out.println(mediaPlayers.size());
                    System.out.println(isClickedArr.size());
                    System.out.println("Filename: " + filename);
                    System.out.println("Sampl: " + alreadySetAudioSamplingRate);
                    System.out.println("Bitr: " + alreadySetAudioEncoderBitRate);
                    return false;
                }
            });

            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (emptyImage.getVisibility() == View.VISIBLE) {
                        emptyImage.startAnimation(fadeOut);
                        emptyImage.setVisibility(View.GONE);
                    }
                    if (temp.exists()) {
                        if (temp.listFiles().length == 1) {
                            stopRecording();
                            File to = new File(savePath + "/" + filename + extension);
                            temp.listFiles()[0].renameTo(to);
                            createCard(myfile.get(myfile.size() - 1), myfile.size() - 1);
                        } else {
                            stopRecording();
                            myfile.remove(myfile.get(myfile.size() - 1));
                            File[] tempRecs = temp.listFiles();
                            File concat = new File(savePath + "/" + filename + extension);
                            mergeSongs(concat, tempRecs);
                            myfile.add(concat);
                            createCard(myfile.get(myfile.size() - 1), myfile.size() - 1);
                            System.out.println("Saving combined file");
                        }
                        try {
                            FileUtils.forceDelete(temp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        stopRecording();
                        System.out.println("Saving " + myfile.get(myfile.size() - 1).getAbsolutePath());
                        createCard(myfile.get(myfile.size() - 1), myfile.size() - 1);
                    }

                    isRecording = false;
                    mNotifyMgr.cancel(1);
                }
            });


            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopRecording();

                    if (temp.exists()) {
                        try {
                            FileUtils.forceDelete(temp);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileUtils.forceDelete(myfile.get(myfile.size() - 1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    myfile.remove(myfile.size() - 1);
                    mNotifyMgr.cancel(1);
                }
            });
        }

    String getCurrentTime() {
        return new SimpleDateFormat("yyyyMMddHHmmss", getResources().getConfiguration().locale).format(new Date());
    }


    void startRecording() {

        if(isSetUp == true){
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(customRecordingName.getWindowToken(), 0);
        }

        pauseAllMediaPlayers();
        settingsView.setVisibility(View.GONE);
        upordown = "down";
        if (customRecordingName.getText().toString().trim().length() > 0) {
            System.out.println("Custom Name Applied: " + customRecordingName.getText().toString());
            filename = customRecordingName.getText().toString();
            customRecordingName.getText().clear();
        } else {
            filename = getCurrentTime();
        }
        recordingFile = new File(savePath + filename + extension);
        if (!new File(savePath).exists()) {
            new File(savePath).mkdir();
        }

        myfile.add(recordingFile);
        System.out.println(myfile.get(myfile.size() - 1));

        time.setBase(SystemClock.elapsedRealtime());
        time.start();
        recordPauseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp));
        saveBtn.setVisibility(View.VISIBLE);
        deleteBtn.setVisibility(View.VISIBLE);

        mRecorder = new MediaRecorder();
        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mRecorder.setAudioSamplingRate(alreadySetAudioSamplingRate);
            mRecorder.setAudioEncodingBitRate(alreadySetAudioEncoderBitRate);
            mRecorder.setAudioChannels(alreadySetAudioChannels);
            mRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mRecorder.setAudioEncoder(alreadySetAudioEncoder);
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();
        isRecording = true;
        isPaused = false;
        System.out.println("RECORDING STARTED");
    }

    void stopRecording() {
        try {
            mNotifyMgr.cancel(1);
            time.stop();
            time.setBase(SystemClock.elapsedRealtime());
            recordPauseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_white_48dp));
            saveBtn.setVisibility(View.INVISIBLE);
            deleteBtn.setVisibility(View.INVISIBLE);

            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;

            isRecording = false;
            isPaused = true;
            System.out.println("RECORDING STOPPED");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void pauseRecording() {
        isPaused = true;
        pauseTime = time.getBase() - SystemClock.elapsedRealtime();
        time.stop();
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;

        if (!temp.exists()) {
            temp.mkdir();
        }
        File to = new File(temp + "/" + filename + extension);
        myfile.get(myfile.size() - 1).renameTo(to);

        recordPauseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp));
        System.out.println("RECORDING PAUSED");
        System.out.println("Saving TEMPORARY " + myfile.get(myfile.size() - 1).getName());
    }

    void resumeRecording() {
        isPaused = false;
        time.setBase(SystemClock.elapsedRealtime() + pauseTime);


        filenameTemp = getCurrentTime();
        recordingFileTemp = new File(temp + "/" + filenameTemp + extension);
        if (!temp.exists()) {
            temp.mkdir();
        }
        time.start();

        mRecorder = new MediaRecorder();
        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mRecorder.setAudioSamplingRate(alreadySetAudioSamplingRate);
            mRecorder.setAudioEncodingBitRate(alreadySetAudioEncoderBitRate);
            mRecorder.setAudioChannels(alreadySetAudioChannels);
            mRecorder.setOutputFile(recordingFileTemp.getAbsolutePath());
            mRecorder.setAudioEncoder(alreadySetAudioEncoder);
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();

        recordPauseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp));
        System.out.println("RECORDING RESUMED");
    }

    void shareRecording(File file) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("audio/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
        sharingIntent.putExtra(Intent.EXTRA_TEXT, "--Recorded with Audio Recorder by Andrei Cozma.");
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.shareWith)));
        System.out.println("Sharing");
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // STORAGE PERMISSION
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadRecordingsFromStorage();
                } else {

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme);
                    dialog.setTitle(R.string.permissionNeeded);
                    dialog.setMessage(getString(R.string.thisAppRequiresAccessToThePhonesStorageInOrderToReadAndWriteAudioFiles) + "\n\n" + getString(R.string.noteIfYouSelectNeverAskAgainYouWillHaveToEnablePermissionsFromTheAppsSettings));
                    dialog.setPositiveButton(R.string.gotIt, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    0);
                            if (Build.VERSION.SDK_INT >= M) {
                                if(!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            }
                        }
                    });
                    dialog.setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    dialog.setCancelable(false);
                    dialog.create();
                    dialog.show();

                }
                return;
            }
            case 1: {
                // MIC PERMISSION
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasMicPermission = true;
                    startRecording();
                } else {
                    Snackbar.make(parentView, R.string.permissionNeeded, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.enable, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.RECORD_AUDIO},
                                            1);
                                    if (Build.VERSION.SDK_INT >= M) {
                                        if(!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivity(intent);
                                        }
                                    }
                                }
                            }).setActionTextColor(getResources().getColor(R.color.colorPrimary)).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void mergeSongs(File mergedFile, File... mp3Files) {
        FileInputStream fisToFinal = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mergedFile);
            fisToFinal = new FileInputStream(mergedFile);
            for (File mp3File : mp3Files) {
                if (!mp3File.exists())
                    continue;
                FileInputStream fisSong = new FileInputStream(mp3File);
                SequenceInputStream sis = new SequenceInputStream(fisToFinal, fisSong);
                byte[] buf = new byte[1024];
                try {
                    for (int readNum; (readNum = fisSong.read(buf)) != -1; )
                        fos.write(buf, 0, readNum);
                } finally {
                    fisSong.close();
                    sis.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (fisToFinal != null) {
                    fisToFinal.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void closeRecordingCard(Integer index){
        System.out.println("Swiped Up");
        pauseAllMediaPlayers();
        for (LinearLayout l : seekBars) {
            if (l.getVisibility() == View.VISIBLE) {
                l.setVisibility(View.GONE);
            }
        }
    }

    void openRecordingCard(Integer index){
        System.out.println("Swiped Down");
        for (LinearLayout l : seekBars) {
            if (l.getVisibility() == View.VISIBLE) {
                l.setVisibility(View.GONE);
            }
        }
        seekBars.get(index).setVisibility(View.VISIBLE);
    }


    void createCard(final File givenFile1, final Integer index) {
        final File[] givenFile = {givenFile1};


        MediaPlayer m = new MediaPlayer();
        mediaPlayers.add(index, m);

        try {
            mediaPlayers.get(index).setDataSource(String.valueOf(Uri.fromFile(givenFile[0])));
            mediaPlayers.get(index).setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayers.get(index).prepare();
            mediaPlayers.get(index).setVolume(100f, 100f);
        } catch (IOException e) {
            e.printStackTrace();
        }


        final View child = getLayoutInflater().inflate(R.layout.card, null);
        parentView.addView(child);


        final CardView cardView = (CardView) child.findViewById(R.id.cardView);
        cardView.setUseCompatPadding(useCompatPadding);
        cardView.setRadius(recCardRadius);
        cardView.setCardElevation(cardElevation);


        TextView cardSize = (TextView) child.findViewById(R.id.cardSize);
        cardSize.setText(getStringSizeLengthFile(givenFile1.length()));

        cardSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBars.get(index).getVisibility() == View.GONE){
                    openRecordingCard(index);
                } else{
                    closeRecordingCard(index);
                }

            }
        });


        TextView cardDate = (TextView) child.findViewById(R.id.cardDate);
        Format dateFormat = android.text.format.DateFormat.getMediumDateFormat(getApplicationContext());
        Format timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        cardDate.setText(dateFormat.format(givenFile[0].lastModified()) + " " + timeFormat.format(givenFile[0].lastModified()));

        cardDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBars.get(index).getVisibility() == View.GONE){
                    openRecordingCard(index);
                } else{
                    closeRecordingCard(index);
                }

            }
        });

        TextView cardTitle = (TextView) child.findViewById(R.id.cardTitle);
        cardTitle.setText(givenFile[0].getName());
        cardTitles.add(index, cardTitle);

        cardTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBars.get(index).getVisibility() == View.GONE){
                    openRecordingCard(index);
                } else{
                    closeRecordingCard(index);
                }

            }
        });

        TextView cardDuration = (TextView) child.findViewById(R.id.cardDuration);
        cardDuration.setText(getTimeString(mediaPlayers.get(index).getDuration()));

        cardDuration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(seekBars.get(index).getVisibility() == View.GONE){
                    openRecordingCard(index);
                } else{
                    closeRecordingCard(index);
                }

            }
        });


        final Boolean[] isClicked = {false};
        isClickedArr.add(index, isClicked[0]);


        final Timer[] timer = new Timer[1];


        final ImageView imageView = (ImageView) child.findViewById(R.id.playPauseBttn);
        imageViews.add(index, imageView);
        imageViews.get(index).setVisibility(View.GONE);

        final TextView currentDuration = (TextView) child.findViewById(R.id.currentDuration);
        final SeekBar seekBar = (SeekBar) child.findViewById(R.id.seekBar);
        seekBar.setProgress(0);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    mediaPlayers.get(index).seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });



        final LinearLayout seekBarLayout = (LinearLayout) child.findViewById(R.id.seekBarLayout);
        seekBars.add(index, seekBarLayout);
        seekBars.get(index).setVisibility(View.GONE);

        child.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
                closeRecordingCard(index);
            }

            public void onSwipeRight() {
                System.out.println("Swiped Right");
                YoYo.with(Techniques.SlideOutRight).duration(300).playOn(child);
                cardDeleteAction(index, givenFile[0], child, "right");
            }

            public void onSwipeLeft() {
                System.out.println("Swiped Left");
                YoYo.with(Techniques.SlideOutLeft).duration(300).playOn(child);
                cardDeleteAction(index, givenFile[0], child, "left");
            }

            public void onSwipeBottom() {
                openRecordingCard(index);
            }

            public void onClick() {
                if (!isClickedArr.get(index)) {
                    pauseAllMediaPlayers();

                    isClicked[0] = true;
                    isClickedArr.set(index, isClicked[0]);

                    imageViews.get(index).setVisibility(View.VISIBLE);

                    cardTitles.get(index).setTextColor(getResources().getColor(R.color.colorRecControls));
                    cardTitles.get(index).setTypeface(null, Typeface.BOLD);

                    mediaPlayers.get(index).start();


                    final Integer maxPosition = mediaPlayers.get(index).getDuration();
                    seekBar.setMax(maxPosition);

                    if (seekBars.get(index).getVisibility() == View.GONE) {
                        seekBars.get(index).setVisibility(View.VISIBLE);
                    }

                    imageViews.get(index).setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white_48dp));

                    final Integer[] currentPosition = {0};
                    timer[0] = new Timer();
                    timer[0].schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (mediaPlayers.get(index).getCurrentPosition() > currentPosition[0]) {
                                    currentPosition[0] = mediaPlayers.get(index).getCurrentPosition();
                                    seekBar.setProgress(currentPosition[0]);
                                    System.out.println("Seekbar progress set to " + currentPosition[0] + " out of " + maxPosition);
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        currentDuration.setText(getTimeString(currentPosition[0]));
                                    }
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, 0, 16);
                } else {
                    isClicked[0] = false;
                    isClickedArr.set(index, isClicked[0]);
                    mediaPlayers.get(index).pause();
                    cardTitles.get(index).setTextColor(getResources().getColor(R.color.colorCardText));
                    cardTitles.get(index).setTypeface(null, Typeface.NORMAL);
                    imageViews.get(index).setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp));
                }
            }
        });


        mediaPlayers.get(index).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                if (mediaPlayers.size() > 0 && mp.isPlaying()) {
                    isClicked[0] = false;
                    isClickedArr.set(index, isClicked[0]);
                    timer[0].cancel();
                    imageViews.get(index).setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp));
                }

            }
        });


        ImageButton cardDelete = (ImageButton) child.findViewById(R.id.cardDelete);
        cardDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardDeleteAction(index, givenFile[0], child, "");
            }
        });

        ImageButton cardRename = (ImageButton) child.findViewById(R.id.cardRename);
        cardRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                pauseAllMediaPlayers();
                final EditText input = new EditText(getApplicationContext());
                input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                input.setTextColor(getResources().getColor(R.color.colorCardText));
                input.setHighlightColor(getResources().getColor(R.color.colorRecControls));
                input.setHintTextColor(getResources().getColor(R.color.colorCardTextLight));
                input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.colorRecControls), PorterDuff.Mode.SRC_ATOP);

                new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme)
                        .setTitle(getString(R.string.renameTo))
                        .setView(input)
                        .setPositiveButton(getString(R.string.rename), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String renamed = input.getText().toString();
                                if (!stringContainsItemFromList(renamed, reservedChars) && !renamed.trim().equals("")) {
                                    File from = new File(givenFile[0].getParentFile().getAbsolutePath(), renamed + extension);
                                    givenFile[0].renameTo(from);
                                    givenFile[0] = from;
                                    System.out.println("Renamed file " + givenFile[0].getName() + " from " + givenFile[0].getParentFile().getPath() + " to " + renamed);
                                    cardTitles.get(index).setText(givenFile[0].getName());

                                    playRandomAnimation(child);

                                } else {
                                    if (renamed.trim().equals("")) {
                                        Snackbar.make(v, getString(R.string.noChangesMade), Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Snackbar.make(v, getString(R.string.cantUseSpecialCharacters), Snackbar.LENGTH_SHORT).show();
                                    }

                                }


                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .create()
                        .show();
            }
        });
        ImageButton cardShare = (ImageButton) child.findViewById(R.id.cardShare);
        cardShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareRecording(givenFile[0]);
            }
        });

    }

    void cardDeleteAction(final Integer index, final File givenFile, final View child, final String direction) {
        pauseAllMediaPlayers();
        new AlertDialog.Builder(MainActivity.this, R.style.DialogTheme)
                .setTitle(getString(R.string.areYouSure))
                .setMessage(cardTitles.get(index).getText() + "\n" + getString(R.string.willBePermanentlyDeleted))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myfile.remove(givenFile);
                        givenFile.getAbsoluteFile().delete();
                        parentView.removeView(child);
                        cardTitles.remove(index);
                        imageViews.remove(index);
                        seekBars.remove(index);
                        mediaPlayers.remove(index);
                        isClickedArr.remove(index);
                        if (myfile.size() == 0 && upordown == "down") {
                            emptyImage.startAnimation(fadeIn);
                            emptyImage.setVisibility(View.VISIBLE);
                        }
                        System.out.println("Deleted file " + givenFile.getName());

                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (direction.equals("left")) {
                            YoYo.with(Techniques.SlideInLeft).duration(300).playOn(child);
                        }
                        if (direction.equals("right")) {
                            YoYo.with(Techniques.SlideInRight).duration(300).playOn(child);
                        }
                    }
                })
                .create()
                .show();
    }


    void pauseAllMediaPlayers() {
        for (final MediaPlayer m : mediaPlayers) {
            if (m.isPlaying()) {
                m.pause();
            }
            if (seekBars.get(mediaPlayers.indexOf(m)).getVisibility() == View.VISIBLE) {
                seekBars.get(mediaPlayers.indexOf(m)).setVisibility(View.GONE);
                imageViews.get(mediaPlayers.indexOf(m)).setVisibility(View.GONE);
                cardTitles.get(mediaPlayers.indexOf(m)).setTextColor(getResources().getColor(R.color.colorCardText));
                cardTitles.get(mediaPlayers.indexOf(m)).setTypeface(null, Typeface.NORMAL);
                imageViews.get(mediaPlayers.indexOf(m)).setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_48dp));
                isClickedArr.set(mediaPlayers.indexOf(m), false);
            }
        }
        System.out.println("All media players paused");
    }


    @Override
    public void onPause() {
        super.onPause();
        pauseAllMediaPlayers();
        System.out.println("Activity Paused");

        if (isRecording) {


            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


            Notification.Builder mBuilder = new Notification.Builder(this)
                    .setContentText(getString(R.string.clickToControlRecording))
                    .setContentIntent(intent)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_mic_white_48dp)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.mipmap.ic_launcher))
                    .setOngoing(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setColor(getResources().getColor(R.color.colorRecCardBackground));
            }
            if (isPaused) {
                mBuilder.setContentTitle(getString(R.string.recordingPaused));
            } else {
                mBuilder.setContentTitle(getString(R.string.stillRecording));
            }
            if (Build.VERSION.SDK_INT >= 21) mBuilder.setVibrate(new long[0]);
            int mNotificationId = 1;
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

        }
    }

    @Override
    public void onBackPressed() {
        if(upordown == "up"){
            closeSettingsView();
        } else if (isRecording){
            moveTaskToBack(true);
        } else{
            finish();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mNotifyMgr.cancel(1);
        for (MediaPlayer m : mediaPlayers) {
            m.release();
        }
        System.out.println("Activity Destroyed");
    }

    public void onResume() {
        super.onResume();
        mNotifyMgr.cancel(1);

    }


}
