package threads.server.fragments;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.utils.MimeType;

import static androidx.core.util.Preconditions.checkNotNull;

public class WebViewDialogFragment extends DialogFragment {

    public static final String TAG = WebViewDialogFragment.class.getSimpleName();
    private static final String DATA = "DATA";
    private static final String TYPE = "TYPE";
    private static final String MIME = "MIME";
    private static final String ENCODING = "ENCODING";

    private Context mContext;

    public static WebViewDialogFragment newInstance(@NonNull String mimeType,
                                                    @NonNull String data,
                                                    @NonNull String encoding) {
        checkNotNull(data);
        checkNotNull(mimeType);
        checkNotNull(encoding);
        Bundle bundle = new Bundle();
        bundle.putString(DATA, data);
        bundle.putString(MIME, mimeType);
        bundle.putString(ENCODING, encoding);

        WebViewDialogFragment fragment = new WebViewDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    public static WebViewDialogFragment newInstance(@NonNull Type type, @NonNull String data) {
        checkNotNull(type);
        checkNotNull(data);
        Bundle bundle = new Bundle();
        bundle.putString(DATA, data);
        bundle.putString(TYPE, type.name());

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
        String argType = args.getString(TYPE);
        if (data != null) {
            WebView wv = new WebView(getContext());
            if (argType != null) {
                Type type = Type.valueOf(argType);

                switch (type) {
                    case HTML:
                        // TODO StandardCharsets.UTF_8.displayName()
                        wv.loadData(data, MimeType.HTML_MIME_TYPE, "UTF-8");
                        break;
                    case TEXT:
                        wv.loadData(data, MimeType.PLAIN_MIME_TYPE, "UTF-8");
                        break;
                    default:
                        wv.loadUrl(data);
                }
            } else {
                String mimeType = args.getString(MIME);
                String encoding = args.getString(ENCODING);
                wv.loadData(data, mimeType, encoding);
            }

            wv.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                    final Uri uri = request.getUrl();
                    v.loadUrl(uri.getPath());
                    return true;
                }

            });

            builder.setView(wv);

            builder.setCancelable(false)
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel());

        }
        return builder.create();

    }


    public enum Type {
        URL, TEXT, HTML
    }
}