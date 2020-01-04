package threads.share;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotesActionDialogFragment extends DialogFragment {
    public static final String TAG = NotesActionDialogFragment.class.getSimpleName();
    private static final String CAMERA_ACTIVE = "CAMERA_ACTIVE";
    private static final String VIDEO_ACTIVE = "VIDEO_ACTIVE";
    private static final String MICRO_ACTIVE = "MICRO_ACTIVE";
    private static final String PDF_ACTIVE = "PDF_ACTIVE";
    private static final String TEXT_ACTIVE = "TEXT_ACTIVE";
    private static final String GALLERY_ACTIVE = "GALLERY_ACTIVE";
    private static final String UPLOAD_ACTIVE = "UPLOAD_ACTIVE";
    private long mLastClickTime = 0;
    private ActionListener mListener;
    private Context mContext;
    private int backgroundColor;


    public static NotesActionDialogFragment newInstance(boolean cameraActive,
                                                        boolean videoActive,
                                                        boolean microActive,
                                                        boolean pdfActive,
                                                        boolean textActive,
                                                        boolean galleryActive,
                                                        boolean uploadActive) {

        Bundle bundle = new Bundle();

        bundle.putBoolean(CAMERA_ACTIVE, cameraActive);
        bundle.putBoolean(VIDEO_ACTIVE, videoActive);
        bundle.putBoolean(MICRO_ACTIVE, microActive);
        bundle.putBoolean(PDF_ACTIVE, pdfActive);
        bundle.putBoolean(TEXT_ACTIVE, textActive);
        bundle.putBoolean(GALLERY_ACTIVE, galleryActive);
        bundle.putBoolean(UPLOAD_ACTIVE, uploadActive);

        NotesActionDialogFragment fragment = new NotesActionDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mListener = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (ActionListener) getActivity();
            backgroundColor = getThemeBackgroundColor(context);
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = activity.getLayoutInflater();

        Bundle args = getArguments();
        checkNotNull(args);

        boolean camActive = args.getBoolean(CAMERA_ACTIVE);
        boolean vidActive = args.getBoolean(VIDEO_ACTIVE);
        boolean micActive = args.getBoolean(MICRO_ACTIVE);
        boolean pdfActive = args.getBoolean(PDF_ACTIVE);
        boolean textActive = args.getBoolean(TEXT_ACTIVE);
        boolean galActive = args.getBoolean(GALLERY_ACTIVE);
        boolean uplActive = args.getBoolean(UPLOAD_ACTIVE);

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_notes_view, null);

        view.setBackgroundColor(backgroundColor);

        TextView menu_gallery = view.findViewById(R.id.menu_gallery);
        if (!galActive) {
            menu_gallery.setVisibility(View.GONE);
        }
        menu_gallery.setOnClickListener((v) -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            mListener.clickMenuGallery();
            dismiss();
        });

        TextView menu_camera = view.findViewById(R.id.menu_camera);
        if (!camActive) {
            menu_camera.setVisibility(View.GONE);
        } else {
            menu_camera.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuImage();
                dismiss();
            });
        }

        TextView menu_video = view.findViewById(R.id.menu_video);
        if (!vidActive) {
            menu_video.setVisibility(View.GONE);
        } else {
            menu_video.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuVideo();
                dismiss();
            });
        }

        TextView menu_micro = view.findViewById(R.id.menu_micro);
        if (!micActive) {
            menu_micro.setVisibility(View.GONE);
        } else {
            menu_micro.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuMicro();
                dismiss();
            });
        }

        TextView menu_pdf = view.findViewById(R.id.menu_pdf);
        if (!pdfActive) {
            menu_pdf.setVisibility(View.GONE);
        } else {
            menu_pdf.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuPDF();
                dismiss();
            });
        }
        TextView menu_text = view.findViewById(R.id.menu_text);
        if (!textActive) {
            menu_text.setVisibility(View.GONE);
        } else {
            menu_text.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuText();
                dismiss();
            });
        }
        TextView menu_upload = view.findViewById(R.id.menu_upload);
        if (!uplActive) {
            menu_upload.setVisibility(View.GONE);
        } else {
            menu_upload.setOnClickListener((v) -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                mListener.clickMenuUpload();
                dismiss();
            });
        }

        if (!camActive && !vidActive && !micActive) {
            view.findViewById(R.id.row_first).setVisibility(View.GONE);
        }
        if (!pdfActive && !textActive && !galActive && !uplActive) {
            view.findViewById(R.id.row_second).setVisibility(View.GONE);
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogBottomAnimation;
            window.getAttributes().gravity = Gravity.BOTTOM | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;
    }

    public interface ActionListener {

        void clickMenuGallery();

        void clickMenuPDF();

        void clickMenuImage();

        void clickMenuMicro();

        void clickMenuVideo();

        void clickMenuUpload();

        void clickMenuText();
    }
}
