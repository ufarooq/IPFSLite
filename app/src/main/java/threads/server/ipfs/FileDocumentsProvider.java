package threads.server.ipfs;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsProvider;
import android.util.Log;

import androidx.annotation.Nullable;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.server.R;
import threads.server.Service;

public class FileDocumentsProvider extends DocumentsProvider {
    private static final String TAG = FileDocumentsProvider.class.getSimpleName();
    private final static String[] DEFAULT_ROOT_PROJECTION =
            new String[]{
                    DocumentsContract.Root.COLUMN_ROOT_ID,
                    DocumentsContract.Root.COLUMN_ICON,
                    DocumentsContract.Root.COLUMN_TITLE,
                    DocumentsContract.Root.COLUMN_FLAGS,
                    DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            };
    // Use these as the default columns to return information about a document if no specific
    // columns are requested in a query.
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
    };
    private THREADS threads;
    private IPFS ipfs;

    /**
     * @param projection the requested root column projection
     * @return either the requested root column projection, or the default projection if the
     * requested projection is null.
     */
    private static String[] resolveRootProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
    }

    private static String[] resolveDocumentProjection(String[] projection) {
        return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_ROOT_PROJECTION);

        String rootId = "threads.server.ipfs";
        String rootDocumentId = "0";
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, rootId);
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        row.add(DocumentsContract.Root.COLUMN_TITLE,
                getContext().getString(R.string.app_name_full));
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_LOCAL_ONLY |
                        DocumentsContract.Root.FLAG_SUPPORTS_CREATE);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootDocumentId);
        return result;
    }

    @Override
    public Cursor queryDocument(String docId, String[] projection) throws FileNotFoundException {

        long idx = Long.parseLong(docId);

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        int flags = 0;
        if (idx == 0) {
            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, docId);
            row.add(Document.COLUMN_DISPLAY_NAME, "ipfs");
            row.add(Document.COLUMN_SIZE, "1000"); // todo
            row.add(Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR);
            row.add(Document.COLUMN_LAST_MODIFIED, new Date());
            row.add(Document.COLUMN_FLAGS, flags);

            // Add a custom icon
            row.add(Document.COLUMN_ICON, R.drawable.server_threads_icon);
        } else {
            Thread file = threads.getThreadByIdx(idx);


            if (false) { // todo when dir
                // Request the folder to lay out as a grid rather than a list. This also allows a larger
                // thumbnail to be displayed for each image.
                //            flags |= Document.FLAG_DIR_PREFERS_GRID;

                // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
                if (file.isMarked() && false) {
                    flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                }
            } else if (file.isMarked()) {
                // If the file is writable set FLAG_SUPPORTS_WRITE and
                // FLAG_SUPPORTS_DELETE
                flags |= Document.FLAG_SUPPORTS_WRITE;
                flags |= Document.FLAG_SUPPORTS_DELETE;
            }

            final String displayName = file.getAdditionalValue(Content.FILENAME);

            final String mimeType = file.getMimeType();

            if (mimeType.startsWith("image/")) {
                // Allow the image to be represented by a thumbnail rather than an icon
                flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
            }

            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, docId);
            row.add(Document.COLUMN_DISPLAY_NAME, displayName);
            row.add(Document.COLUMN_SIZE, "1000"); // todo
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add(Document.COLUMN_LAST_MODIFIED, new Date());
            row.add(Document.COLUMN_FLAGS, flags);

            // Add a custom icon
            row.add(Document.COLUMN_ICON, R.drawable.server_threads_icon);
        }


        return result;

    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {

        long idx = Long.parseLong(parentDocumentId);

        List<Thread> entries = threads.getThreadsByThread(idx);

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        for (Thread file : entries) {

            int flags = 0;
            if (false) { // todo when dir
                // Request the folder to lay out as a grid rather than a list. This also allows a larger
                // thumbnail to be displayed for each image.
                //            flags |= Document.FLAG_DIR_PREFERS_GRID;

                // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
                if (file.isMarked() && false) {
                    flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
                }
            } else if (file.isMarked()) {
                // If the file is writable set FLAG_SUPPORTS_WRITE and
                // FLAG_SUPPORTS_DELETE
                flags |= Document.FLAG_SUPPORTS_WRITE;
                flags |= Document.FLAG_SUPPORTS_DELETE;
            }

            final String displayName = file.getAdditionalValue(Content.FILENAME);
            final String mimeType = file.getMimeType();

            if (mimeType.startsWith("image/")) {
                // Allow the image to be represented by a thumbnail rather than an icon
                flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
            }

            final MatrixCursor.RowBuilder row = result.newRow();
            row.add(Document.COLUMN_DOCUMENT_ID, "" + file.getIdx());
            row.add(Document.COLUMN_DISPLAY_NAME, displayName);
            row.add(Document.COLUMN_SIZE, "1000"); // todo
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add(Document.COLUMN_LAST_MODIFIED, new Date());
            row.add(Document.COLUMN_FLAGS, flags);

            // Add a custom icon
            row.add(Document.COLUMN_ICON, R.drawable.server_threads_icon);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {

        long idx = Long.parseLong(documentId);
        Thread file = threads.getThreadByIdx(idx);

        final int accessMode = ParcelFileDescriptor.parseMode(mode);
        final boolean isWrite = (mode.indexOf('w') != -1);

        if (!isWrite) {
            CID cid = file.getCid();
            try {
                InputStream inputStream = ipfs.getStream(cid, 1000, true);

                return ParcelFileDescriptorUtil.pipeFrom(inputStream);
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        return null;
    }

    @Override
    public boolean onCreate() {

        Service.getInstance(getContext());

        threads = Singleton.getInstance(getContext()).getThreads();

        ipfs = Singleton.getInstance(getContext()).getIpfs();
        return true;
    }

    private static class ParcelFileDescriptorUtil {
        public static ParcelFileDescriptor pipeFrom(InputStream inputStream)
                throws IOException {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

            new TransferThread(inputStream, output).start();

            return pipe[0];
        }

        @SuppressWarnings("unused")
        public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
                throws IOException {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            final InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);


            new TransferThread(input, outputStream).start();

            return pipe[1];
        }
    }

    static class TransferThread extends java.lang.Thread {
        final InputStream mIn;
        final OutputStream mOut;

        TransferThread(InputStream in, OutputStream out) {
            super("ParcelFileDescriptor Transfer Thread");
            mIn = in;
            mOut = out;
        }

        @Override
        public void run() {
            try {
                IOUtils.copy(mIn, mOut);
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
    }
}