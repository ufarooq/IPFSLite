package threads.server.fragments;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;
import threads.server.utils.MimeType;

import static androidx.core.util.Preconditions.checkNotNull;

public class WebViewDialogFragment extends DialogFragment {

    public static final String TAG = WebViewDialogFragment.class.getSimpleName();
    private static final String DATA = "DATA";
    private Context mContext;


    public static WebViewDialogFragment newInstance(@NonNull String data) {
        checkNotNull(data);
        Bundle bundle = new Bundle();
        bundle.putString(DATA, data);

        WebViewDialogFragment fragment = new WebViewDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        Bundle args = getArguments();
        checkNotNull(args);
        String data = args.getString(DATA);

        WebView wv = new WebView(getContext());
        wv.loadData(data, MimeType.HTML_MIME_TYPE, "UTF-8");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                final Uri uri = request.getUrl();
                v.loadUrl(uri.getPath());
                return true;
            }

        });

        builder.setView(wv);
        builder.setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel());


        Dialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogRightAnimation;
            window.getAttributes().gravity = Gravity.CENTER;
        }

        return dialog;

    }
}