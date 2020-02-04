package threads.server.services;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.util.Size;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Optional;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.utils.MimeType;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class ThumbnailService {

    private static final String TAG = ThumbnailService.class.getSimpleName();
    private static final int THUMBNAIL_SIZE = 128;


    @NonNull
    public static byte[] getImage(@NonNull Context context, @DrawableRes int id) {
        checkNotNull(context);
        Drawable drawable = context.getDrawable(id);
        checkNotNull(drawable);
        return getImage(drawable);
    }

    public static byte[] getImage(@NonNull String displayName) {
        String letter = displayName.substring(0, 1);
        int color = ColorGenerator.MATERIAL.getColor(displayName);
        Canvas canvas = new Canvas();
        TextDrawable drawable = TextDrawable.builder()
                .buildRound(letter, color);
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, 64, 64);

        drawable.draw(canvas);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }


    @NonNull
    public static CID getImage(@NonNull Context context,
                               @NonNull String name,
                               @DrawableRes int id) throws Exception {
        checkNotNull(context);
        checkNotNull(name);
        byte[] data = getImageData(context, name, id);
        final IPFS ipfs = IPFS.getInstance(context);
        checkNotNull(ipfs);
        CID cid = ipfs.storeData(data);
        checkNotNull(cid);
        return cid;
    }

    @NonNull
    private static byte[] getImageData(@NonNull Context context, @NonNull String name, @DrawableRes int id) {
        checkNotNull(context);
        checkNotNull(name);
        Drawable drawable = context.getDrawable(id);
        checkNotNull(drawable);
        int color = ColorGenerator.MATERIAL.getColor(name);
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        return getImage(drawable);
    }

    @NonNull
    private static byte[] getImage(@NonNull Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, 64, 64);

        drawable.draw(canvas);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }


    @NonNull
    private static Bitmap getPDFBitmap(@NonNull Context context, @NonNull Uri uri) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);

        ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(
                uri, "r");
        checkNotNull(fileDescriptor);
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
        checkNotNull(context);
        checkNotNull(uri);

        Log.e(TAG, uri.toString());

        ContentResolver contentResolver = context.getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType == null) {
            mimeType = MimeType.OCTET_MIME_TYPE;
        }
        Log.e(TAG, "" + mimeType);
        try (Cursor cursor = contentResolver.query(uri, new String[]{
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE}, null, null, null)) {

            checkNotNull(cursor);
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
        checkNotNull(context);
        checkNotNull(file);
        checkNotNull(mimeType);


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
        checkNotNull(context);
        checkNotNull(file);
        checkNotNull(mimeType);

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

    private static Bitmap getBitmap(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            input.close();
            return bitmap;
        }
    }

    public static class FileDetails {

        @NonNull
        private final String fileName;
        @NonNull
        private final String mimeType;
        private final long fileSize;

        private FileDetails(@NonNull String fileName, @NonNull String mimeType, long fileSize) {
            checkNotNull(fileName);
            checkNotNull(mimeType);
            checkArgument(fileSize > -1);
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
