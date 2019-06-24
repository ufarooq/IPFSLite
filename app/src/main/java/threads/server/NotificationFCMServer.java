package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import threads.core.Singleton;
import threads.core.api.Content;

import static androidx.core.util.Preconditions.checkNotNull;

//import com.google.auth.oauth2.AccessToken;
//import com.google.auth.oauth2.GoogleCredentials;


public class NotificationFCMServer implements Singleton.NotificationServer {
    private static final String TAG = NotificationFCMServer.class.getSimpleName();
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String SERVICE = "https://fcm.googleapis.com/v1/projects/threads-server/messages:send";
    private static NotificationFCMServer SINGELTON = new NotificationFCMServer();

    private NotificationFCMServer() {
    }

    static NotificationFCMServer getInstance() {
        return SINGELTON;
    }

    @Nullable
    private String getAccessToken(@NonNull Context context) {
        checkNotNull(context);
        /*
        try {

            InputStream inputStream = context.getResources().openRawResource(R.raw.threads_server);
            GoogleCredentials googleCredential = GoogleCredentials
                    .fromStream(inputStream);
            if (googleCredential.createScopedRequired()) {
                googleCredential = googleCredential.createScoped(
                        Collections.singleton(SCOPE));
            }

            AccessToken token = googleCredential.refreshAccessToken();

            return token.getTokenValue();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        */
        return null;
    }


    public boolean sendNotification(@NonNull Context context,
                                    @NonNull String token,
                                    @NonNull String pid,
                                    @NonNull Map<String, String> params) {
        checkNotNull(context);
        checkNotNull(token);
        checkNotNull(pid);


        String accessToken = getAccessToken(context);
        if (accessToken != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(Content.PID,
                    StringEscapeUtils.escapeJava(pid));

            for (String key : params.keySet()) {
                String content = params.get(key);
                if (content != null) {
                    jsonObject.addProperty(key, StringEscapeUtils.escapeJava(content));
                }
            }


            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("token", token);
            jsonObj.add("data", jsonObject);

            JsonObject msgObj = new JsonObject();
            msgObj.add("message", jsonObj);


            return sendMessageToFcm(accessToken, msgObj.toString());
        }
        return false;
    }

    private boolean sendMessageToFcm(@NonNull String accessToken, @NonNull String postData) {
        HttpURLConnection httpConn = null;
        boolean result = true;
        try {
            httpConn = getConnection(accessToken);

            DataOutputStream wr = new DataOutputStream(httpConn.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();
            wr.close();


            InputStream inputStream = httpConn.getInputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(inputStream));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            result = false;
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return result;
    }

    private HttpURLConnection getConnection(@NonNull String accessToken) throws Exception {
        checkNotNull(accessToken);
        URL url = new URL(SERVICE);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setDoInput(true);
        httpURLConnection.setUseCaches(false);
        httpURLConnection.setRequestMethod("POST");
        return httpURLConnection;
    }

}
