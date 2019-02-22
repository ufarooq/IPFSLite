package threads.server;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import threads.core.Preferences;
import threads.core.mdl.EventViewModel;


public class InfoFragment extends Fragment {


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.webui_view, container, false);

        WebView wv = view.findViewById(R.id.webview);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setBuiltInZoomControls(true);
        wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        wv.setScrollbarFadingEnabled(false);
        wv.getSettings().setUseWideViewPort(true);


        if (getActivity() != null) {
            wv.loadUrl(Preferences.getWebUI(getActivity()));
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return false;
            }
        });

        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);


        eventViewModel.getIPFSServerOnlineEvent().observe(this, (event) -> {

            try {
                if (event != null) {
                    if (getActivity() != null) {
                        wv.loadUrl(Preferences.getWebUI(getActivity()));
                    }
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        return view;
    }
}
