package threads.share;

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
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.j256.simplemagic.ContentInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Optional;

import threads.core.threads.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class ThumbnailService {

    private static final String TAG = ThumbnailService.class.getSimpleName();
    private static final int THUMBNAIL_SIZE = 128;

    @Nullable
    public static CID createResourceImage(@NonNull Context context,
                                          @NonNull IPFS ipfs,
                                          @DrawableRes int id) throws Exception {
        checkNotNull(context);
        checkNotNull(ipfs);
        byte[] data = getImage(context, id);
        return ipfs.storeData(data);
    }

    @Nullable
    public static CID createNameImage(@NonNull IPFS ipfs,
                                      @NonNull String name) throws Exception {
        checkNotNull(name);
        checkNotNull(ipfs);
        byte[] data = getImage(name);
        return ipfs.storeData(data);
    }

    @Nullable
    public static Bitmap getImage(@NonNull IPFS ipfs, @NonNull Thread thread,
                                  int timeout, boolean offline) {
        checkNotNull(ipfs);
        checkNotNull(thread);
        CID image = thread.getThumbnail();

        if (image != null) {
            return getImage(ipfs, image, timeout, offline);
        }
        return null;
    }

    @Nullable
    public static Bitmap getImage(@NonNull IPFS ipfs,
                                  @NonNull CID image,
                                  int timeout,
                                  boolean offline) {
        checkNotNull(ipfs);
        checkNotNull(image);

        try {
            byte[] bytes = ipfs.getData(image, timeout, offline);
            if (bytes != null) {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            return null;
        } catch (Throwable e) {
            return null;
        }

    }

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
    public static Bitmap getNameImage(@NonNull String name) {
        checkNotNull(name);
        Canvas canvas = new Canvas();
        String letter = name.substring(0, 1);
        int color = ColorGenerator.MATERIAL.getColor(name);
        TextDrawable drawable = TextDrawable.builder()
                .buildRound(letter, color);
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, 64, 64);

        drawable.draw(canvas);
        return bitmap;
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

    @Nullable
    private static Bitmap getPreview(@NonNull Context context, @NonNull Uri uri) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);

        FileDetails fileDetails = getFileDetails(context, uri);
        checkNotNull(fileDetails);
        String mimeType = fileDetails.getMimeType();

        if (mimeType.startsWith("video")) {


            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, uri);

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
            return getPDFBitmap(context, uri);
        } else if (mimeType.startsWith("image")) {

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            return ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        }
        return null;
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
    private static byte[] getPreviewImage(@NonNull Context context, @NonNull Uri uri) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);
        Bitmap bitmap = getPreview(context, uri);
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            byte[] image = stream.toByteArray();
            bitmap.recycle();
            return image;
        }
        return null;
    }

    public static Bitmap getBitmapThumbnail(@NonNull Context context, @NonNull Uri uri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {

                return context.getContentResolver().loadThumbnail(
                        uri, new Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null);


            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        } else {
            try {

                return getThumbnailBitmap(context, uri);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    @Nullable
    public static FileDetails getFileDetails(@NonNull Context context,
                                             @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);

        Log.e(TAG, uri.toString());
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor cursor = contentResolver.query(uri, new String[]{
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE}, null, null, null)) {

            checkNotNull(cursor);
            cursor.moveToFirst();

            String mimeType = cursor.getString(0);
            if (mimeType == null) {
                mimeType = MimeType.OCTET_MIME_TYPE;
            }
            String fileName = cursor.getString(1);
            if (fileName == null) {
                fileName = "";
            }
            long fileSize = cursor.getLong(2);
            return FileDetails.create(fileName, mimeType, fileSize);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


        return null;
    }

    public static Bitmap getThumbnailBitmap(Context context, Uri uri) {
        String[] proj = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};


        Cursor cursor = MediaStore.Images.Thumbnails.queryMiniThumbnails(
                context.getContentResolver(), uri, MediaStore.Images.Thumbnails.MINI_KIND, proj);

        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        cursor.moveToFirst();
        long imageId = cursor.getLong(column_index);

        cursor.close();

        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                context.getContentResolver(), imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null);

        return bitmap;
    }

    @NonNull
    public static Result getThumbnail(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);

        CID cid = null;
        boolean thumbnail = false;
        byte[] bytes = null;


        try {
            bytes = getPreviewImage(context, uri);
            if (bytes != null) {
                thumbnail = true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }

        if (bytes != null) {
            final IPFS ipfs = IPFS.getInstance(context);

                try {
                    cid = ipfs.storeData(bytes);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }

        }
        if (cid == null) {
            thumbnail = false;
        }
        return new Result(cid, thumbnail);
    }


    public static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    @NonNull
    public static Result getThumbnail(@NonNull Context context,
                                      @NonNull File file,
                                      @NonNull String mimeType) throws Exception {
        checkNotNull(context);
        checkNotNull(file);
        checkNotNull(mimeType);

        Bitmap bitmap = null;
        byte[] bytes = null;
        CID cid = null;
        boolean thumbnail = false;


        if (!mimeType.isEmpty()) {

            bitmap = getPreview(context, file, mimeType);
            if (bitmap != null) {
                thumbnail = true;
            }


        }

        if (bitmap == null) {
            IPFS ipfs = IPFS.getInstance(context);
            if (ipfs != null) {
                ContentInfo contentInfo = ipfs.getContentInfo(file);
                if (contentInfo != null) {
                    mimeType = contentInfo.getMimeType();
                    if (mimeType != null) {
                        bitmap = getPreview(context, file, mimeType);
                    }
                }
            }
        }


        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            bytes = stream.toByteArray();
            bitmap.recycle();
        }

        if (bytes != null) {
            final IPFS ipfs = IPFS.getInstance(context);
            if (ipfs != null) {
                try {
                    cid = ipfs.storeData(bytes);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        }
        if (cid == null) {
            thumbnail = false;
        }
        return new Result(cid, thumbnail);

    }

    @Nullable
    private static Bitmap getPreview(@NonNull Context context,
                                     @NonNull File file,
                                     @NonNull String mimeType) throws Exception {
        checkNotNull(context);
        checkNotNull(file);
        checkNotNull(mimeType);


        if (mimeType.startsWith("video")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                return ThumbnailUtils.createVideoThumbnail(file, new Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null);
            } else {
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(context, Uri.fromFile(file));

                // mediaMetadataRetriever.getPrimaryImage(); // TODO support in the future
                Bitmap bitmap;
                try {
                    String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    Long timeUs = Long.decode(time);
                    long timeFrame = (timeUs / 100) * 5;

                    bitmap = mediaMetadataRetriever.getFrameAtTime(timeFrame);
                } catch (Throwable e) {
                    bitmap = mediaMetadataRetriever.getFrameAtTime();
                }
                mediaMetadataRetriever.release();
                return bitmap;
            }
        } else if (mimeType.startsWith("application/pdf")) {
            return getPDFBitmap(context, Uri.fromFile(file));
        } else if (mimeType.startsWith("image")) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                return ThumbnailUtils.createImageThumbnail(file,
                        new Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null);
            } else {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        context.getContentResolver(), Uri.fromFile(file));
                return ThumbnailUtils.extractThumbnail(
                        bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            }
        }

        return null;

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
        public String getFileName() {
            return fileName;
        }

        @NonNull
        public String getMimeType() {
            return mimeType;
        }

        public long getFileSize() {
            return fileSize;
        }

    }

    public static class Result {
        @Nullable
        private final CID cid;
        private final boolean thumbnail;

        Result(@Nullable CID cid, boolean thumbnail) {
            this.cid = cid;
            this.thumbnail = thumbnail;
        }

        @Nullable
        public CID getCid() {
            return cid;
        }

        public boolean isThumbnail() {
            return thumbnail;
        }

    }
}
