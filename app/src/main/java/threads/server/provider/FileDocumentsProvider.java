package threads.server.provider;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mobile.Reader;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.server.BuildConfig;
import threads.server.R;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static androidx.core.util.Preconditions.checkNotNull;

public class FileDocumentsProvider extends DocumentsProvider {
    private static final String TAG = FileDocumentsProvider.class.getSimpleName();
    private final static long SPLIT = (long) 1e+7;
    private final static String[] DEFAULT_ROOT_PROJECTION =
            new String[]{
                    DocumentsContract.Root.COLUMN_ROOT_ID,
                    DocumentsContract.Root.COLUMN_ICON,
                    DocumentsContract.Root.COLUMN_TITLE,
                    DocumentsContract.Root.COLUMN_FLAGS,
                    DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            };
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
    };
    private static final int QR_CODE_SIZE = 250;
    private String appName;
    private THREADS threads;
    private IPFS ipfs;


    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    public static Uri getUriForThread(Thread thread) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content")
                .authority(BuildConfig.DOCUMENTS_AUTHORITY)
                .appendPath("document")
                .appendPath("" + thread.getIdx());
        return builder.build();
    }

    public static Uri getUriForString(String hash) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content")
                .authority(BuildConfig.DOCUMENTS_AUTHORITY)
                .appendPath("document")
                .appendPath(hash);
        return builder.build();
    }

    public static Bitmap getBitmap(@NonNull String hash) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(hash,
                    BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Cursor queryRoots(String[] projection) {
        MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));

        String rootId = BuildConfig.DOCUMENTS_AUTHORITY;
        String rootDocumentId = "0";
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, rootId);
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        row.add(DocumentsContract.Root.COLUMN_TITLE, appName);
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_LOCAL_ONLY |
                        DocumentsContract.Root.FLAG_SUPPORTS_RECENTS |
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootDocumentId);
        return result;
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection) {

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));


        List<Thread> entries = threads.getNewestThreadsByStatus(Status.SEEDING, 5);
        for (Thread thread : entries) {
            includeFile(result, thread);
        }

        return result;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection) {
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));


        List<Thread> entries = threads.getThreadsByQuery(Status.SEEDING, query);
        for (Thread thread : entries) {
            includeFile(result, thread);
        }

        return result;
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
                                                     CancellationSignal signal)
            throws FileNotFoundException {

        long idx = Long.parseLong(documentId);

        Thread file = threads.getThreadByIdx(idx);
        if (file == null) {
            throw new FileNotFoundException();
        }
        CID cid = file.getThumbnail();
        if (cid == null) {
            throw new FileNotFoundException();
        }
        try {
            if (signal != null) {
                if (signal.isCanceled()) {
                    return null;
                }
            }

            if (file.getSize() < SPLIT) {
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                        getContentFile(cid), MODE_READ_ONLY);
                return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            } else {

                final ParcelFileDescriptor pfd = ParcelFileDescriptorUtil.pipeFrom(ipfs, cid, signal);
                return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }

        } catch (Throwable e) {
            throw new FileNotFoundException(e.getLocalizedMessage());
        }
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        try {
            long idx = Long.parseLong(docId);

            int flags = 0;
            if (idx == 0) {
                final MatrixCursor.RowBuilder row = result.newRow();
                row.add(Document.COLUMN_DOCUMENT_ID, docId);
                row.add(Document.COLUMN_DISPLAY_NAME, "ipfs");
                row.add(Document.COLUMN_SIZE, null);
                row.add(Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR);
                row.add(Document.COLUMN_LAST_MODIFIED, new Date());
                row.add(Document.COLUMN_FLAGS, flags);


            } else {
                Thread file = threads.getThreadByIdx(idx);
                if (file == null) {
                    throw new FileNotFoundException();
                }
                includeFile(result, file);
            }
        } catch (NumberFormatException e) {

            try {

                Multihash.fromBase58(docId);


                final MatrixCursor.RowBuilder row = result.newRow();
                row.add(Document.COLUMN_DOCUMENT_ID, docId);
                row.add(Document.COLUMN_DISPLAY_NAME, docId);
                row.add(Document.COLUMN_SIZE, null);
                row.add(Document.COLUMN_MIME_TYPE, "image/png");
                row.add(Document.COLUMN_LAST_MODIFIED, new Date());
                row.add(Document.COLUMN_FLAGS, 0);

            } catch (Throwable throwable) {
                throw new FileNotFoundException("" + throwable.getLocalizedMessage());
            }
        }


        return result;

    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {

        try {
            long idx = Long.parseLong(documentId);
            if (idx == 0) {
                return Document.MIME_TYPE_DIR;
            } else {
                Thread file = threads.getThreadByIdx(idx);
                if (file == null) {
                    throw new FileNotFoundException();
                }
                return file.getMimeType();
            }
        } catch (NumberFormatException e) {
            try {
                Multihash.fromBase58(documentId);
                return "image/png";
            } catch (Throwable throwable) {
                throw new FileNotFoundException("" + throwable.getLocalizedMessage());
            }
        }
    }

    private void includeFile(MatrixCursor result, Thread file) {
        int flags = 0;

        final String displayName = file.getName();
        final String mimeType = file.getMimeType();

        if (file.hasImage()) {
            flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }

        final MatrixCursor.RowBuilder row = result.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, "" + file.getIdx());
        row.add(Document.COLUMN_DISPLAY_NAME, displayName);
        row.add(Document.COLUMN_SIZE, file.getSize());
        row.add(Document.COLUMN_MIME_TYPE, mimeType);
        row.add(Document.COLUMN_LAST_MODIFIED, file.getLastModified());
        row.add(Document.COLUMN_FLAGS, flags);

    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {

        try {
            long idx = Long.parseLong(parentDocumentId);

            List<Thread> entries = threads.getChildrenByStatus(idx, Status.SEEDING);

            final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

            for (Thread file : entries) {
                includeFile(result, file);
            }
            return result;
        } catch (Throwable e) {
            throw new FileNotFoundException("" + e.getLocalizedMessage());
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId,
                                             String mode,
                                             @Nullable CancellationSignal signal) throws FileNotFoundException {

        final int accessMode = ParcelFileDescriptor.parseMode(mode);

        try {
            long idx = Long.parseLong(documentId);
            Thread file = threads.getThreadByIdx(idx);

            if (file == null) {
                throw new FileNotFoundException("");
            }

            CID cid = file.getContent();
            if (cid == null) {
                throw new FileNotFoundException("");
            }
            try {
                if (file.getSize() < SPLIT) {
                    return ParcelFileDescriptor.open(getContentFile(cid), accessMode);
                } else {
                    return ParcelFileDescriptorUtil.pipeFrom(ipfs, cid, signal);
                }
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        } catch (NumberFormatException e) {
            try {
                Multihash.fromBase58(documentId);

                File impl = getBitmapFile(documentId);

                return ParcelFileDescriptor.open(impl, accessMode);
            } catch (Throwable throwable) {
                throw new FileNotFoundException("" + throwable.getLocalizedMessage());
            }
        }

        return null;
    }

    private File getContentFile(@NonNull CID cid) throws IOException {

        File file = new File(ipfs.getCacheDir(), cid.getCid());
        if (!file.exists()) {
            ipfs.storeToFile(file, cid);
        }
        return file;
    }

    private File getBitmapFile(@NonNull String hash) throws IOException {
        Bitmap bitmap = getBitmap(hash);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] bytes = stream.toByteArray();
        bitmap.recycle();
        File file = new File(ipfs.getCacheDir(), hash);
        if (!file.exists()) {
            FileUtils.writeByteArrayToFile(file, bytes);
        }
        return file;
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        checkNotNull(context);
        appName = context.getString(R.string.app_name);
        threads = THREADS.getInstance(context);
        ipfs = IPFS.getInstance(getContext());
        return true;
    }


    private static class ParcelFileDescriptorUtil {

        static ParcelFileDescriptor pipeFrom(@NonNull IPFS ipfs,
                                             @NonNull CID cid,
                                             @Nullable CancellationSignal signal)
                throws Exception {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (OutputStream output =
                             new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    storeToOutputStream(ipfs, output, cid, signal);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
            return pipe[0];
        }

        private static void storeToOutputStream(@NonNull IPFS ipfs,
                                                @NonNull OutputStream os,
                                                @NonNull CID cid,
                                                @Nullable CancellationSignal signal) throws Exception {
            checkNotNull(ipfs);
            checkNotNull(cid);
            checkNotNull(os);
            Reader reader = ipfs.getReader(cid);
            try {
                int size = 262158;
                reader.load(size);
                long read = reader.getRead();
                while (read > 0) {
                    byte[] bytes = reader.getData();
                    if (signal != null) {
                        if (signal.isCanceled()) {
                            return;
                        }
                    }
                    os.write(bytes, 0, bytes.length);
                    reader.load(size);
                    read = reader.getRead();
                }
            } finally {
                reader.close();
            }

        }
    }


}