package threads.server.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.utils.MimeType;

public class ThumbnailService {

    private static final String TAG = ThumbnailService.class.getSimpleName();
    private static final int THUMBNAIL_SIZE = 128;


    @NonNull
    private static Bitmap getPDFBitmap(@NonNull Context context, @NonNull Uri uri) throws Exception {

        ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(
                uri, "r");
        Objects.requireNonNull(fileDescriptor);
        PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

        PdfRenderer.Page rendererPage = pdfRenderer.openPage(0);
        int rendererPageWidth = rendererPage.getWidth();
        int rendererPageHeight = rendererPage.getHeight();


        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);
        rendererPage.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);


        rendererPage.close();

        pdfRenderer.close();
        fileDescriptor.close();
        return bitmap;
    }


    @Nullable
    static FileDetails getFileDetails(@NonNull Context context, @NonNull Uri uri) {


        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType == null) {
            mimeType = MimeType.OCTET_MIME_TYPE;
        }
        Log.e(TAG, "" + mimeType);
        try (Cursor cursor = contentResolver.query(uri, new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE}, null, null, null)) {

            Objects.requireNonNull(cursor);
            cursor.moveToFirst();

            String fileName = cursor.getString(0);
            if (fileName == null) {
                fileName = "";
            }
            long fileSize = cursor.getLong(1);
            return FileDetails.create(fileName, mimeType, fileSize);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


        return null;
    }


    public static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    @Nullable
    public static CID getThumbnail(@NonNull Context context,
                                   @NonNull File file,
                                   @NonNull String mimeType) {


        try {
            Bitmap bitmap = getPreview(context, file, mimeType);
            byte[] bytes = null;


            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
                bytes = stream.toByteArray();
                bitmap.recycle();
            }

            if (bytes != null) {
                final IPFS ipfs = IPFS.getInstance(context);


                return ipfs.storeData(bytes);


            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;

    }


    @Nullable
    private static Bitmap getPreview(@NonNull Context context, @NonNull File file, @NonNull String mimeType) {


        try {
            if (mimeType.startsWith("video")) {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(context, Uri.fromFile(file));

                Bitmap bitmap = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {

                    try {
                        bitmap = mediaMetadataRetriever.getPrimaryImage();
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage());
                    }
                }
                try {
                    if (bitmap == null) {
                        final WeakReference<Bitmap> weakBmp = new WeakReference<>(mediaMetadataRetriever.getFrameAtTime());
                        bitmap = weakBmp.get();
                    }
                } catch (Throwable e) {
                    bitmap = mediaMetadataRetriever.getFrameAtTime();
                }
                mediaMetadataRetriever.release();
                return bitmap;
            } else if (mimeType.startsWith("application/pdf")) {
                return getPDFBitmap(context, Uri.fromFile(file));
            } else if (mimeType.startsWith("image")) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    return ThumbnailUtils.createImageThumbnail(file,
                            new Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null);
                } else {
                    Bitmap bitmap = getBitmap(file);
                    return ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return null;

    }

    private static Bitmap getBitmap(@NonNull File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return BitmapFactory.decodeStream(input);
        }
    }

    public static class FileDetails {

        @NonNull
        private final String fileName;
        @NonNull
        private final String mimeType;
        private final long fileSize;

        private FileDetails(@NonNull String fileName, @NonNull String mimeType, long fileSize) {

            this.fileName = fileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }

        public static FileDetails create(@NonNull String fileName,
                                         @NonNull String mimeType,
                                         long fileSize) {
            return new FileDetails(fileName, mimeType, fileSize);
        }

        @NonNull
        String getFileName() {
            return fileName;
        }

        @NonNull
        public String getMimeType() {
            return mimeType;
        }

        long getFileSize() {
            return fileSize;
        }

    }
}
