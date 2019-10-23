package com.example.facedetection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.FaceDetector;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate
import android.Manifest;
import android.util.Size;
import android.graphics.Matrix;
import java.util.concurrent.TimeUnit;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.overlay.OverlayLayout;
import static android.graphics.Bitmap.Config.RGB_565;
import static android.graphics.Bitmap.createBitmap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    int maxNumFaces =2;
    int imageWidth =2000;
    int imageHeight = 2000;
    int i = 0;
    double x1 = 0;
    double x2 = 0;
    double y1 = 0;
    double y2 = 0 ;
    int iterator1 =0;
    int iterator2 = 0;
    boolean firstX = false;
    boolean firstY = false;
    boolean secondX = false;
    boolean secondY = false;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA= 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkCameraPermission();
        //Part of Camera2 api
//        textureView = (TextureView) findViewById(R.id.texture);
        textView =  (TextView) findViewById(R.id.text);
//        assert textureView != null;
//        textureView.setSurfaceTextureListener(textureListener);
//        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
//        assert takePictureButton != null;
//        takePictureButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                takePicture();
//            }
//        });


        //Using com.otaliastudios camera API Instead Will attempt to fall back to the other camera system.

        CameraView cameraview = (CameraView) findViewById(R.id.cameraView);
        cameraview.setFacing(Facing.FRONT);
        cameraview.setLifecycleOwner(this); //Automatically handles the camera lifecycle
        cameraview.addFrameProcessor(new FrameProcessor() {
            @Override
            @WorkerThread
            public void process(@NonNull Frame frame) {
                byte[] data = frame.getData();
                int rotation = frame.getRotation();
                long time = frame.getTime();
                com.otaliastudios.cameraview.size.Size size = frame.getSize();
                int format = frame.getFormat();

                // Process...
                FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(data, extractFrameMetadata(frame));
                detectFaces(image);
            }
        });
    }
    private FirebaseVisionImageMetadata extractFrameMetadata (Frame frame){

        return new FirebaseVisionImageMetadata.Builder()
                .setWidth(frame.getSize().getWidth())
                .setHeight(frame.getSize().getHeight())
                .setFormat(frame.getFormat())
                .setRotation(frame.getRotation() / 90)
                .build();
    }

//    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
//        @Override
//        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            //open your camera here
//            openCamera();
////            imageWidth = textureView.getBitmap().getWidth();
////            imageHeight = textureView.getBitmap().getHeight();
//        }
//        @Override
//        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//            // Transform you image captured size according to the surface width and height
//        }
//        @Override
//        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            return false;
//        }
//        @Override
//        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//            System.out.println("Update TIme" +surface.getTimestamp());
//
//
//
//           final Bitmap image = textureView.getBitmap();
////
//            detectFaces( imageFromBitmap(image));
////            image.setConfig(RGB_565);
//            Thread t = new Thread(new Runnable() {
//                public void run() {
//                    /*
//                     * Do something
//                     *
//                     */
//
////                    PointF point = new PointF();
////                    int numFacesFound = fd.findFaces(image, faces);
////                System.out.println("NUMBER OF FACES" + numFacesFound);
////
////            saveBitmap("Test",textureView.getBitmap() );
////                for (int i = 0; i< numFacesFound; i++)
////                {
////                    faces[i].confidence();
////                    faces[i].getMidPoint(point);
////                    faces[i].pose(0);
////                    faces[i].eyesDistance();
////                    Log.d("confidence: ", faces[i].confidence() + "");
////                    Log.d("eyedistance: ", faces[i].eyesDistance () + "");
////                    Log.d("EyeDistance: ",faces[i].eyesDistance() +"" );
////
////
////                }
//                    final Bitmap image = textureView.getBitmap();
////
//                    detectFaces( imageFromBitmap(image));
//                }
//            });
//
//            t.start();
////            t.stop();
//
//           // Bitmap d =  createBitmap(imageWidth, imageHeight, Bitmap.Config.RGB_565);
//
////                int numFacesFound = fd.findFaces(image, faces);
////                System.out.println("NUMBER OF FACES" + numFacesFound);
////            saveBitmap("Test",textureView.getBitmap() );
////                for (int i = 0; i< numFacesFound; i++)
////                {
////                    faces[i].confidence();
////                    faces[i].getMidPoint(point);
////                    faces[i].pose(0);
////                    faces[i].eyesDistance();
////                    Log.d("confidence: ", faces[i].confidence() + "");
////                    Log.d("eyedistance: ", faces[i].eyesDistance () + "");
////                    Log.d("EyeDistance: ",faces[i].eyesDistance() +"" );
////
////                }
//
//
//
//        }
//    };
//
//    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
//        @Override
//        public void onOpened(CameraDevice camera) {
//            //This is called when the camera is open
//            Log.e(TAG, "onOpened");
//            cameraDevice = camera;
//            createCameraPreview();
//        }
//        @Override
//        public void onDisconnected(CameraDevice camera) {
//            cameraDevice.close();
//        }
//        @Override
//        public void onError(CameraDevice camera, int error) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//    };
//    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
//        @Override
//        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//            super.onCaptureCompleted(session, request, result);
//            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
//            createCameraPreview();
//        }
//    };
//    protected void startBackgroundThread() {
//        mBackgroundThread = new HandlerThread("Camera Background");
//        mBackgroundThread.start();
//        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
//    }
//    protected void stopBackgroundThread() {
//        mBackgroundThread.quitSafely();
//        try {
//            mBackgroundThread.join();
//            mBackgroundThread = null;
//            mBackgroundHandler = null;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
    public static void saveBitmap( String bitName,
                                  Bitmap mBitmap) {

        File f = new File(Environment.getExternalStorageDirectory()
                .toString() + "/" + bitName + ".png");
        try {
            f.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    protected void takePicture() {
//        if(null == cameraDevice) {
//            Log.e(TAG, "cameraDevice is null");
//            return;
//        }
//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        try {
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
//            Size[] jpegSizes = null;
//            if (characteristics != null) {
//                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
//            }
//            int width = 640;
//            int height = 480;
//            if (jpegSizes != null && 0 < jpegSizes.length) {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//            }
//            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
//            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
//            outputSurfaces.add(reader.getSurface());
//            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(reader.getSurface());
//            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//            // Orientation
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
//            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
//                @Override
//                public void onImageAvailable(ImageReader reader) {
//                    Image image = null;
//                    try {
//                        image = reader.acquireLatestImage();
//                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//                        byte[] bytes = new byte[buffer.capacity()];
//                        buffer.get(bytes);
//                        save(bytes);
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    } finally {
//                        if (image != null) {
//                            image.close();
//                        }
//                    }
//                }
//                private void save(byte[] bytes) throws IOException {
//                    OutputStream output = null;
//                    try {
//                        output = new FileOutputStream(file);
//                        output.write(bytes);
//                    } finally {
//                        if (null != output) {
//                            output.close();
//                        }
//                    }
//                }
//            };
//            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
//            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
//                @Override
//                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
//                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
//                    createCameraPreview();
//                }
//            };
//            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
//                @Override
//                public void onConfigured(CameraCaptureSession session) {
//                    try {
//                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//                @Override
//                public void onConfigureFailed(CameraCaptureSession session) {
//                }
//            }, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//    protected void createCameraPreview() {
//        try {
//            SurfaceTexture texture = textureView.getSurfaceTexture();
//            assert texture != null;
//            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
//            Surface surface = new Surface(texture);
//            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
//            captureRequestBuilder.addTarget(surface);
//            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
//                @Override
//                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    //The camera is already closed
//                    if (null == cameraDevice) {
//                        return;
//                    }
//                    // When the session is ready, we start displaying the preview.
//                    System.out.println("SYSTEM PREVIEW");
//                    cameraCaptureSessions = cameraCaptureSession;
//                    updatePreview();
//                }
//                @Override
//                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
//                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
//                }
//            }, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//    private void openCamera() {
//        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        Log.e(TAG, "is camera open");
//        try {
//            cameraId = manager.getCameraIdList()[1];
//            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
//            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            assert map != null;
//            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
//            // Add permission for camera and let user grant the permission
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
//                return;
//            }
//            manager.openCamera(cameraId, stateCallback, null);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//        Log.e(TAG, "openCamera X");
//    }
//    protected void updatePreview() {
//        if(null == cameraDevice) {
//            Log.e(TAG, "updatePreview error, return");
//        }
//        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
//        try {
//            System.out.println("SYSTEM UPDATE 2");
//            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//    }
//    private void closeCamera() {
//        if (null != cameraDevice) {
//            cameraDevice.close();
//            cameraDevice = null;
//        }
//        if (null != imageReader) {
//            imageReader.close();
//            imageReader = null;
//        }
//    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
//        startBackgroundThread();
//        if (textureView.isAvailable()) {
//            openCamera();
//        } else {
//            textureView.setSurfaceTextureListener(textureListener);
//        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
//        stopBackgroundThread();
        super.onPause();
    }

//    private FirebaseVisionImageMetadata extractFrameMetadata (Frame frame){
//
//        return new FirebaseVisionImageMetadata.Builder()
//                .setWidth(frame.getSize().getWidth())
//                .setHeight(frame.getSize().getHeight())
//                .setFormat(frame.getFormat())
//                .setRotation(frame.getRotation() / 90)
//                .build();
//    }
    private void detectFaces(FirebaseVisionImage image) {
        // [START set_detector_options]
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setMinFaceSize(0.15f)
                        .enableTracking()
                        .build();
        // [END set_detector_options]

        // [START get_detector]
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);
        // [END get_detector]

        // [START run_detector]
        Task<List<FirebaseVisionFace>> result =
                detector.detectInImage(image)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        String output = "";
                                        // Task completed successfully
                                        // [START_EXCLUDE]
                                        // [START get_face_info]
                                        for (FirebaseVisionFace face : faces) {
                                            Rect bounds = face.getBoundingBox();
                                            float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
//                                            Log.d("roty", rotY + "");
                                            float rotZ = face.getHeadEulerAngleZ();
//                                            Log.d("rotZ", rotZ + "");
                                            FirebaseVisionFaceLandmark mouthButtom = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                                            FirebaseVisionPoint mouthButtomPosition = mouthButtom.getPosition();
//                                            Log.d("MOUTH pos", mouthButtom.toString());

                                            output = ""+yes((double)mouthButtomPosition.getX(), (double) mouthButtomPosition.getY());


                                            // If classification was enabled:

//                                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//
//                                                float smileProb = face.getSmilingProbability();
//                                            }
////                                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
//
//                                                float rightEyeOpenProb = face.getRightEyeOpenProbability();
//                                            }
                                            // If face tracking was enabled:

                                            if (face.getTrackingId() != FirebaseVisionFace.INVALID_ID) {

                                                int id = face.getTrackingId();

                                            }



                                            /*** Draw Bounding Box ***/
//
//                                            ImageView overlay = findViewById(R.id.boundBox);
//                                            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) overlay.getLayoutParams();
//                                            params.width = bounds.width();
//                                            params.height = bounds.height();
//
//                                            if(bounds.left <= bounds.right){
//                                                overlay.setTop(bounds.top);
//                                                overlay.setLeft(bounds.left);
//                                                overlay.setBottom(bounds.top + bounds.width());
//                                                overlay.setRight(bounds.left + bounds.height());
//                                            }

                                        }
                                        // [END get_face_info]
                                        // [END_EXCLUDE]
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        // [END run_detector]
    }
    private FirebaseVisionImage imageFromBitmap(Bitmap bitmap) {
        // [START image_from_bitmap]
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        // [END image_from_bitmap]
        return image;
    }
    public boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_camera_permission)
                        .setMessage(R.string.text_camera_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                        MY_PERMISSIONS_REQUEST_CAMERA);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }
            return false;
        } else {
            return true;
        }
    }

    public double distanceFormula(double x1,  double y1) {

        i = 0;
        if(i == 0)
        {
            this.x1 = x1;
            this.y1 = y1;
            i++;
        }
        else
        {
            this.y2 = y1;
            this.x2 = x1;
            i = 0;
        }

        double distance = 0;

        distance = Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));

        return distance;

    }
    public boolean yes(double x, double y)
    {

        boolean result = false;

        if(iterator1 == 0)
        {
            x1 = x;
            y1 = y;
            iterator1++;
        }
        else if( iterator1 ==1)
        {
            x2 = x;
            y2 = y;
            iterator1 ++;
        }
        else {
            x1 = x2;
            x2 = x;
            y1 = y2;
            y2 = y;
            iterator1 = 2;
        }

        if( iterator1 != 0  && distance(x2,x1) >15 && distance(y2,y1) >(-.10 * Math.abs(distance(x2,x1))) && distance(y2,y1) <(.10 * Math.abs(distance(x2,x1))) && firstY)
        {
            System.out.println("X1 " +x1 +" Y1 " +y1);
            System.out.println("X2 " +x2 +" Y2 " +y2);
            System.out.println("RIGHT");
            textView.setText(" NO "+result);
            firstX = true;
            firstY = false;
        }

        if( iterator1 != 0  && distance(x2,x1) <-15 && distance(y2,y1) >(-.10 * Math.abs(distance(x2,x1))) && distance(y2,y1) <(.10 * Math.abs(distance(x2,x1))) )
        {
            System.out.println("X1 " +x1 +" Y1 " +y1);
            System.out.println("X2 " +x2 +" Y2 " +y2);
            System.out.println("LEFT");
//            textView.setText(" NO "+result);
            firstY = true;
        }
        if( iterator1 != 0  && distance(y2,y1) >15 && distance(x2,x1) >(-.10 * Math.abs(distance(y2,y1))) && distance(x2,x1) <(.10 * Math.abs(distance(y2,y1))))
        {
            System.out.println("X1 " +x1 +" Y1 " +y1);
            System.out.println("X2 " +x2 +" Y2 " +y2);
            System.out.println("UP");
            //textView.setText(" Yes "+result);
            firstX = true;
        }
        if( iterator1 != 0  && distance(y2,y1) <-15 && distance(x2,x1) >(-.10 * Math.abs(distance(y2,y1))) && distance(x2,x1) <(.10 * Math.abs(distance(y2,y1))) && firstX)
        {
            System.out.println("X1 " +x1 +" Y1 " +y1);
            System.out.println("X2 " +x2 +" Y2 " +y2);
            System.out.println("DOWN");
            textView.setText(" Yes "+result);

        }
//


        return result;
    }

    public double distance(double val1, double val2)
    {
        return val2 -val1;
    }


}