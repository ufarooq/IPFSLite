package threads.server;

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;


public class ScannerDialogFragment extends DialogFragment implements CameraSourcePreview.OnCameraErrorListener {

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private ScannerListener mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ScannerListener) {
            mCallback = (ScannerListener) context;
        } else {
            throw new ClassCastException("Activity hosting the QRScanFragment must implement the ScannerListener interface");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barcode, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreview = view.findViewById(R.id.preview);
        mPreview.setOnCameraErrorListener(this);
        onCreateDetector(view);
    }

    private void onCreateDetector(@NonNull final View view) {
        final Context context = view.getContext().getApplicationContext();
        final BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(context).build();
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(new MultiProcessor.Factory<Barcode>() {
            @Override
            public Tracker<Barcode> create(final Barcode barcode) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        String qrCode = barcode.displayValue;
                        if (qrCode != null) {
                            try {
                                mCallback.handleScannerCode(qrCode);
                            } catch (Throwable e) {
                                mCallback.onCameraError(R.string.camera_error_decrypt_failed);
                            } finally {
                                mPreview.stop();
                                dismiss();
                            }
                        }
                    }
                });
                return new Tracker<>();
            }
        }).build());

        if (!barcodeDetector.isOperational()) {
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            if (context.registerReceiver(null, lowStorageFilter) != null) {
                // Low storage
                mCallback.onCameraError(R.string.camera_error_low_storage);
            } else {
                // Native libs unavailable
                mCallback.onCameraError(R.string.camera_error_dependencies);
            }
            return;
        }

        final ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);


                CameraSource.Builder builder = new CameraSource.Builder(context, barcodeDetector)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(view.getMeasuredWidth(), view.getMeasuredHeight())
                        .setRequestedFps(30.0f);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    builder = builder.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }

                mCameraSource = builder.build();
                startCameraSource();
            }
        });
    }

    public void restart() {
        startCameraSource();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    private void startCameraSource() {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (Throwable e) {
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onCameraError() {
        if (mCallback != null) {
            mCallback.onCameraError(R.string.camera_error_open);
        }
    }


}
