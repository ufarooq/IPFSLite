package threads.server.services;

import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import threads.server.R;
import threads.server.utils.MimeType;

public class MimeTypeService {


    public static int getMediaResource(@NonNull String mimeType) {

        if (!mimeType.isEmpty()) {
            if (mimeType.equals(MimeType.OCTET_MIME_TYPE)) {
                return R.drawable.file_star;
            }
            if (mimeType.equals(MimeType.PLAIN_MIME_TYPE)) {
                return R.drawable.text_file;
            }
            if (mimeType.startsWith("text")) {
                return R.drawable.text_file;
            }
            if (mimeType.equals(MimeType.PDF_MIME_TYPE)) {
                return R.drawable.text_pdf;
            }
            if (mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                return R.drawable.text_folder;
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
            if (mimeType.startsWith("application")) {
                return R.drawable.application;
            }

            return R.drawable.settings;
        }

        return R.drawable.help;

    }
}
