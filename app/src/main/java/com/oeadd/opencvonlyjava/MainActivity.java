package com.oeadd.opencvonlyjava;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getSimpleName();
    //    JavaCameraView cameraView;
    private JavaCameraView mOpenCvCameraView;
    private Mat mRgba;
    private Mat mGray;
    private Mat mFilp;
    private Mat mTranspose;
    private ImageView imageView;
    private TextView mArea;
    private TextView mPointSize;
    private TextView mWidth;
    private TextView mHeight;
    private TextView mCardSide;
    private RelativeLayout mMainContent;
    private ImageView mIDCardBg;
    private boolean isIdCardFrontGet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.img_z);
        mArea = findViewById(R.id.area);
        mPointSize = findViewById(R.id.pointsize);
        mWidth = findViewById(R.id.width);
        mHeight = findViewById(R.id.height);
        mCardSide = findViewById(R.id.cardside);
        mOpenCvCameraView = findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mMainContent = findViewById(R.id.mainContent);
        mIDCardBg = findViewById(R.id.idcard_bg);
//        cameraView = findViewById(R.id.cameraView);
//        cameraView.setCvCameraViewListener(this);
        //权限请求
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission
                .requestEachCombined(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(permission -> {
                    if (permission.granted) {
                    } else if (permission.shouldShowRequestPermissionRationale) {
                    } else {
                        mOpenCvCameraView.enableView();
                    }
                });
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        mFilp = new Mat(width, height, CvType.CV_8UC4);
        mTranspose = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
//        Mat blur = new Mat();
//        Mat canny = new Mat();
//        List<MatOfPoint> matOfPoint = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.GaussianBlur(mGray, blur, new Size(5, 5), 0);
//        Imgproc.Canny(blur, canny, 50, 100, 3, false);
//        Imgproc.findContours(canny, matOfPoint, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
//        double maxVal = 0;
//        int maxValIdx = 0;
//        for (int contourIdx = 0; contourIdx < matOfPoint.size(); contourIdx++) {
//            double contourArea = Imgproc.contourArea(matOfPoint.get(contourIdx));
//            if (maxVal < contourArea) {
//                maxVal = contourArea;
//                maxValIdx = contourIdx;
//            }
//        }
//        if (matOfPoint.size() != 0) {
//            double contourArea = Imgproc.contourArea(matOfPoint.get(maxValIdx));
//            List<Point> points = matOfPoint.get(maxValIdx).toList();
//            double x, y, width, height;
//            width = height = 0;
//            x = points.get(0).x;
//            y = points.get(0).y;
//            for (int i = 0; i < points.size(); i++) {
//                if (points.get(i).x > width) {
//                    width = points.get(i).x;
//                }
//                if (points.get(i).x < x) {
//                    x = points.get(i).x;
//                }
//                if (points.get(i).y > height) {
//                    height = points.get(i).y;
//                }
//                if (points.get(i).y < y) {
//                    y = points.get(i).y;
//                }
//            }
//            width = width - x;
//            height = height - y;
//            int finalMaxValIdx = maxValIdx;
//            double finalHeight = height;
//            double finalY = y;
//            double finalWidth = width;
//            double finalX = x;
//            if (points.size() > 1250 && points.size() < 1450 && contourArea > 105000f && contourArea < 130000f) {
//                if (isIdCardFrontGet) {
//                    return mRgba;
//                }
//                if (height > 430 && height < 466 && width > 260 && width < 286) {
//                    Bitmap bitmap2 = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(mRgba, bitmap2);
//
//                }
//            }
//        }
        if (!isIdCardFrontGet){
            runOnUiThread(() -> {
                Bitmap bitmap = IDCardCheckUtils.checkIDCard(inputFrame);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    isIdCardFrontGet = true;
                }
            });
        }
        return mRgba;
    }
}
