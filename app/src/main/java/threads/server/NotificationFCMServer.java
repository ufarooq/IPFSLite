package threads.server;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.gson.JsonObject;

import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

import threads.core.Preferences;
import threads.core.api.Content;
import threads.ipfs.Network;
import threads.share.NotificationServer;

import static androidx.core.util.Preconditions.checkNotNull;


public class NotificationFCMServer implements NotificationServer {
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String SERVICE = "https://fcm.googleapis.com/v1/projects/threads-server/messages:send";
    private static final NotificationFCMServer INSTANCE = new NotificationFCMServer();

    private NotificationFCMServer() {
    }

    static NotificationFCMServer getInstance() {
        return INSTANCE;
    }

    @NonNull
    public static String getAccessToken(@NonNull Context context, @RawRes int id) {
        checkNotNull(context);
        String token = "";
        try {

            InputStream inputStream = context.getResources().openRawResource(id);
            GoogleCredential googleCredential = GoogleCredential
                    .fromStream(inputStream);
            if (googleCredential.createScopedRequired()) {
                googleCredential = googleCredential.createScoped(
                        Collections.singleton(SCOPE));
            }

            googleCredential.refreshToken();

            token = googleCredential.getAccessToken();

        } catch (Throwable e) {
            if (Network.isConnected(context)) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }
        return token;
    }

    public boolean sendNotification(@NonNull String accessToken,
                                    @NonNull String token,
                                    @NonNull String pid) {
        checkNotNull(accessToken);
        checkNotNull(token);
        checkNotNull(pid);


        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(Content.PID,
                StringEscapeUtils.escapeJava(pid));
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("token", token);
        jsonObj.add("data", jsonObject);

        JsonObject msgObj = new JsonObject();
        msgObj.add("message", jsonObj);


        return sendMessageToFcm(accessToken, msgObj.toString());
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
