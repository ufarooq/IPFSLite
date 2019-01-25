package threads.server;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static com.google.common.base.Preconditions.checkNotNull;

public class WebViewDialogFragment extends DialogFragment {

    public static final String TAG = WebViewDialogFragment.class.getSimpleName();
    private static final String ARG_URL = "ARG_URL";


    public static WebViewDialogFragment newInstance(@NonNull String url) {
        checkNotNull(url);
        Bundle bundle = new Bundle();
        bundle.putString(ARG_URL, url);

        WebViewDialogFragment fragment = new WebViewDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        Bundle args = getArguments();
        checkNotNull(args);
        String url = args.getString(ARG_URL);
        if (url != null) {
            WebView wv = new WebView(getContext());
            wv.loadUrl(url);
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    final Uri uri = request.getUrl();
                    view.loadUrl(uri.getPath());
                    return true;
                }

            });

            builder.setView(wv);

            builder.setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel());

        }
        return builder.create();
    }


}