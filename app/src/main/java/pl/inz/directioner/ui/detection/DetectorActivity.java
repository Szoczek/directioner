package pl.inz.directioner.ui.detection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import pl.inz.directioner.R;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public abstract class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(0, 0);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;
    private Classifier detector;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private boolean computingDetection = false;
    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private BorderedText borderedText;
    private boolean isLearning;

    public DetectorActivity(boolean isLearning) {
        this.isLearning = isLearning;
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);

            setNumThreads(4);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> tracker.draw(canvas));

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);

    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                () -> {
                    LOGGER.i("Running detection on image " + currTimestamp);
                    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    final Canvas canvas1 = new Canvas(cropCopyBitmap);
                    final Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(2.0f);

                    float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    if (MODE == DetectorMode.TF_OD_API) {
                        minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    }

                    final List<Classifier.Recognition> mappedRecognitions =
                            new LinkedList<>();

                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= minimumConfidence) {
                            canvas1.drawRect(location, paint);

                            cropToFrameTransform.mapRect(location);

                            result.setLocation(location);
                            mappedRecognitions.add(result);
                        }
                    }

                    compareFrames(lastImageResults, mappedRecognitions);

                    tracker.trackResults(mappedRecognitions, currTimestamp);
                    trackingOverlay.postInvalidate();


                    lastImageResults.clear();
                    lastImageResults.addAll(mappedRecognitions);
                    computingDetection = false;
                });
    }

    private List<Classifier.Recognition> lastImageResults = new ArrayList<>();
    ArrayList<String> vehicles = new ArrayList<String>(
            Arrays.asList("bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat"));

    ArrayList<String> animals = new ArrayList<String>(
            Arrays.asList("bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe"));

    private void compareFrames(List<Classifier.Recognition> lastFrameSet, List<Classifier.Recognition> currentFrameSet) {
        if (lastFrameSet.isEmpty() || currentFrameSet.isEmpty())
            return;

        currentFrameSet.forEach(x ->
                lastFrameSet.forEach(y -> {
                    float previewSize = previewHeight * previewWidth;
                    float heightX = Math.abs(x.getLocation().top - x.getLocation().bottom);
                    float widthX = Math.abs(x.getLocation().left - x.getLocation().right);
                    float zSize = heightX * widthX;
                    float diff = previewSize / zSize;

                    if (diff < 4)
                        objectTooClose();


                    boolean danger = vehicles.contains(x.getTitle()) || animals.contains(x.getTitle());
                    if (Objects.equals(x.getTitle(), y.getTitle()) && danger) {
                        float centerXDiff = Math.abs(y.getLocation().centerX() - x.getLocation().centerX());
                        float centerYDiff = Math.abs(y.getLocation().centerY() - x.getLocation().centerY());

                        if (centerXDiff < 50 || centerYDiff < 50) {
                            float heightY = Math.abs(y.getLocation().top - y.getLocation().bottom);
                            float heightDiff = heightY - heightX;
                            if (heightDiff > 100)
                                dangerAhead();
                        }
                    }
                })
        );
    }

    abstract public void objectTooClose();

    abstract public void dangerAhead();

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected int getContainerId() {
        if (isLearning)
            return R.id.learnContainer;
        else
            return R.id.routeContainer;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }
}
