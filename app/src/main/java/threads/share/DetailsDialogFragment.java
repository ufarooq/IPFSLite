package threads.share;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class DetailsDialogFragment extends DialogFragment {

    public static final String TAG = DetailsDialogFragment.class.getSimpleName();
    private static final String DATA = "DATA";
    private static final String TYPE = "TYPE";
    private static final String MIME = "MIME";
    private static final String ENCODING = "ENCODING";

    private Context mContext;

    public static DetailsDialogFragment newInstance(@NonNull String mimeType,
                                                    @NonNull String data,
                                                    @NonNull String encoding) {
        checkNotNull(data);
        checkNotNull(mimeType);
        checkNotNull(encoding);
        Bundle bundle = new Bundle();
        bundle.putString(DATA, data);
        bundle.putString(MIME, mimeType);
        bundle.putString(ENCODING, encoding);

        DetailsDialogFragment fragment = new DetailsDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    public static DetailsDialogFragment newInstance(@NonNull Type type, @NonNull String data) {
        checkNotNull(type);
        checkNotNull(data);
        Bundle bundle = new Bundle();
        bundle.putString(DATA, data);
        bundle.putString(TYPE, type.name());

        DetailsDialogFragment fragment = new DetailsDialogFragment();
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
        checkNotNull(data);
        WebView wv = new WebView(getContext());
        if (argType != null) {
            Type type = Type.valueOf(argType);

            switch (type) {
                case HTML:
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

        Dialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;


    }


    public enum Type {
        URL, TEXT, HTML
    }
}