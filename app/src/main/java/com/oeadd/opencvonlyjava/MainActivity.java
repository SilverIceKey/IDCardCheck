package com.oeadd.opencvonlyjava;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.tbruyelle.rxpermissions2.RxPermissions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

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
    private boolean isIdCardFrontGet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.img);
        mArea = findViewById(R.id.area);
        mPointSize = findViewById(R.id.pointsize);
        mWidth = findViewById(R.id.width);
        mHeight = findViewById(R.id.height);
        mOpenCvCameraView = findViewById(R.id.cameraView);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
        Mat cannyMat = new Mat();
        Mat blur = new Mat();
        Mat thresh = new Mat();
        List<MatOfPoint> matOfPoint = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.GaussianBlur(mGray, blur, new Size(5, 5), 0);
        Imgproc.Canny(blur, thresh, 50, 100, 3,false);
        Imgproc.findContours(thresh, matOfPoint, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < matOfPoint.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(matOfPoint.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
//        mRgba.create(thresh.rows(), thresh.cols(), CvType.CV_8UC3);
        //绘制检测到的轮廓
        Imgproc.drawContours(mRgba, matOfPoint, maxValIdx, new Scalar(0, 255, 0), 5);
        if (matOfPoint.size() != 0) {
            double contourArea = Imgproc.contourArea(matOfPoint.get(maxValIdx));
            List<Point> points = matOfPoint.get(maxValIdx).toList();
            if (points.size() > 1000 && points.size() < 1900&&contourArea>110000f&&contourArea<130000f) {
                if (isIdCardFrontGet){
                    return mRgba;
                }
                isIdCardFrontGet = true;
                double x, y, width, height;
                width = height = 0;
                x = points.get(0).x;
                y = points.get(0).y;
                for (int i = 0; i < points.size(); i++) {
                    if (points.get(i).x > width) {
                        width = points.get(i).x;
                    }
                    if (points.get(i).x < x) {
                        x = points.get(i).x;
                    }
                    if (points.get(i).y > height) {
                        height = points.get(i).y;
                    }
                    if (points.get(i).y < y) {
                        y = points.get(i).y;
                    }
                }
                width = width-x;
                height = height-y;
                int finalMaxValIdx = maxValIdx;
                double finalHeight = height;
                double finalY = y;
                double finalWidth = width;
                double finalX = x;
                if (height>450&&width>270&&width<300){
                    Bitmap bitmap2 = Bitmap.createBitmap(mRgba.cols(),mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bitmap2);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mArea.setText("contourArea->" + Imgproc.contourArea(matOfPoint.get(finalMaxValIdx)));
                            mPointSize.setText("pointsize->" + points.size());
                            mWidth.setText("width->" + (finalHeight));
                            mHeight.setText("height->" + (finalWidth));
                        Bitmap bitmap = Bitmap.createBitmap(bitmap2,(int) finalX, (int) finalY , (int) (finalWidth) ,(int) (finalHeight), null, false);
//                        ImageView tmpImageView = new ImageView(MainActivity.this);
//                        tmpImageView.setImageBitmap(bitmap);

                        imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            }
        }
        return mRgba;
    }

    //高斯滤波
    public static Mat removeNoiseGaussianBlur(Mat srcMat) {
        final Mat blurredImage = new Mat();
        Size size = new Size(7, 7);
        Imgproc.GaussianBlur(srcMat, blurredImage, size, 0, 0);
        return blurredImage;
    }
}
