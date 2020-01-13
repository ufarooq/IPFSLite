package threads.share;

import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class MimeTypeService {

    public static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }


    public static int getMediaResource(@NonNull String mimeType) {
        checkNotNull(mimeType);
        if (!mimeType.isEmpty()) {

            if (mimeType.equals(MimeType.PLAIN_MIME_TYPE)) {
                return R.drawable.text_file;
            }
            if (mimeType.equals(MimeType.PDF_MIME_TYPE)) {
                return R.drawable.text_pdf;
            }

            if (mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                return R.drawable.text_folder;
            }
            // todo
            if (mimeType.equals("qr_code")) {
                return R.drawable.text_qrcode;
            }
            if (mimeType.startsWith("video")) {
                return R.drawable.text_video;
            }
            if (mimeType.startsWith("image")) {
                return R.drawable.text_camera;
            }
            if (mimeType.startsWith("audio")) {
                return R.drawable.text_audio;
            }
            if (mimeType.equals(MimeType.TORRENT_MIME_TYPE)) {
                return R.drawable.text_torrent;
            }
        }

        return R.drawable.text_file;

    }
}
