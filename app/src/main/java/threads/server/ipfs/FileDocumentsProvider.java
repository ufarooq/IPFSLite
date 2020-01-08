package threads.server.ipfs;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import mobile.Reader;
import threads.core.Singleton;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.server.BuildConfig;
import threads.server.R;
import threads.server.Service;

import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

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
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final int MAX_LAST_MODIFIED = 5;
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

    public static Uri getUriForThread(Thread thread) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("content")
                .authority(BuildConfig.DOCUMENTS_AUTHORITY)
                .appendPath("document")
                .appendPath("" + thread.getIdx());
        return builder.build();
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        MatrixCursor result = new MatrixCursor(
                projection != null ? projection : DEFAULT_ROOT_PROJECTION);

        String rootId = BuildConfig.DOCUMENTS_AUTHORITY;
        String rootDocumentId = "0";
        MatrixCursor.RowBuilder row = result.newRow();
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, rootId);
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher);
        row.add(DocumentsContract.Root.COLUMN_TITLE, getContext().getString(R.string.app_name));
        row.add(DocumentsContract.Root.COLUMN_FLAGS,
                DocumentsContract.Root.FLAG_LOCAL_ONLY |
                        DocumentsContract.Root.FLAG_SUPPORTS_RECENTS |
                        DocumentsContract.Root.FLAG_SUPPORTS_SEARCH);
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, rootDocumentId);
        return result;
    }

    @Override
    public Cursor queryRecentDocuments(String rootId, String[] projection)
            throws FileNotFoundException {
        Log.v(TAG, "queryRecentDocuments");
        // TODO optimize on native search in database

        // This example implementation walks a local file structure to find the most recently
        // modified files.  Other implementations might include making a network call to query a
        // server.

        // Create a cursor with the requested projection, or the default projection.
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));


        List<Thread> entries = threads.getThreads();


        // Create a queue to store the most recent documents, which orders by last modified.
        PriorityQueue<Thread> lastModifiedFiles = new PriorityQueue<Thread>(5, new Comparator<Thread>() {
            public int compare(Thread i, Thread j) {
                return Long.compare(i.getLastModified(), j.getLastModified());
            }
        });

        // Iterate through all files and directories in the file structure under the root.  If
        // the file is more recent than the least recently modified, add it to the queue,
        // limiting the number of results.
        final LinkedList<Thread> pending = new LinkedList<Thread>();

        // Start by adding the parent to the list of files to be processed
        pending.addAll(entries);

        // Do while we still have unexamined files
        while (!pending.isEmpty()) {
            // Take a file from the list of unprocessed files
            final Thread file = pending.removeFirst();
            if (!file.isDir()) {
                lastModifiedFiles.add(file);
            }
        }

        // Add the most recent files to the cursor, not exceeding the max number of results.
        int includedCount = 0;
        while (includedCount < MAX_LAST_MODIFIED + 1 && !lastModifiedFiles.isEmpty()) {
            final Thread file = lastModifiedFiles.remove();
            includeFile(result, file);
            includedCount++;
        }
        return result;
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection)
            throws FileNotFoundException {
        Log.v(TAG, "querySearchDocuments");
        // TODO optimize on native search in database

        // Create a cursor with the requested projection, or the default projection.
        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
        List<Thread> entries = threads.getThreads();

        // This example implementation searches file names for the query and doesn't rank search
        // results, so we can stop as soon as we find a sufficient number of matches.  Other
        // implementations might use other data about files, rather than the file name, to
        // produce a match; it might also require a network call to query a remote server.

        // Iterate through all files in the file structure under the root until we reach the
        // desired number of matches.
        final LinkedList<Thread> pending = new LinkedList<Thread>();

        // Start by adding the parent to the list of files to be processed
        pending.addAll(entries);

        // Do while we still have unexamined files, and fewer than the max search results
        while (!pending.isEmpty() && result.getCount() < MAX_SEARCH_RESULTS) {
            // Take a file from the list of unprocessed files
            final Thread file = pending.removeFirst();
            if (!file.isDir()) {

                final String displayName = file.getName();

                // If it's a file and it matches, add it to the result cursor.
                if (displayName.toLowerCase().contains(query)) {
                    includeFile(result, file);
                }
            }
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
            File impl = new File(ipfs.getCacheDir(), "" + idx);

            storeToFile(impl, cid, signal);

            if (signal != null) {
                if (signal.isCanceled()) {
                    return null;
                }
            }

            final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(impl,
                    MODE_READ_ONLY);
            return new AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH);

        } catch (Throwable e) {
            throw new FileNotFoundException(e.getLocalizedMessage());
        }
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
            row.add(Document.COLUMN_SIZE, null); // todo
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


        return result;

    }

    @Override
    public String getDocumentType(String documentId) throws FileNotFoundException {

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
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {

        long idx = Long.parseLong(documentId);
        if (idx != 0) {
            Thread file = threads.getThreadByIdx(idx);
            if (file == null) {
                throw new FileNotFoundException();// todo message
            }
            threads.removeThread(ipfs, file);
        }
    }

    private void includeFile(MatrixCursor result, Thread file) {
        int flags = 0;
        if (file.isDir()) {
            // Request the folder to lay out as a grid rather than a list. This also allows a larger
            // thumbnail to be displayed for each image.
            //            flags |= Document.FLAG_DIR_PREFERS_GRID;

            // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
            if (file.isDir() && false) {
                flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
            }
        } else if (file.isMarked() && false) {
            // If the file is writable set FLAG_SUPPORTS_WRITE and
            // FLAG_SUPPORTS_DELETE
            flags |= Document.FLAG_SUPPORTS_WRITE;
            flags |= Document.FLAG_SUPPORTS_DELETE;
        }

        final String displayName = file.getName();

        final String mimeType = file.getMimeType();

        if (file.hasImage()) {
            // Allow the image to be represented by a thumbnail rather than an icon
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

        long idx = Long.parseLong(parentDocumentId);

        List<Thread> entries = threads.getThreadsByThread(idx); // todo only valid threads

        final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));

        for (Thread file : entries) {
            includeFile(result, file);
        }
        return result;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {

        long idx = Long.parseLong(documentId);
        Thread file = threads.getThreadByIdx(idx);

        if (file == null) {
            throw new FileNotFoundException("");
        }
        final int accessMode = ParcelFileDescriptor.parseMode(mode);

        CID cid = file.getContent();
        if (cid == null) {
            throw new FileNotFoundException("");
        }
        try {

            File impl = new File(ipfs.getCacheDir(), "" + idx);


            storeToFile(impl, cid, signal);


            if (signal != null) {
                if (signal.isCanceled()) {
                    return null;
                }
            }
            return ParcelFileDescriptor.open(impl, accessMode);
            //return ParcelFileDescriptorUtil.pipeFrom(ipfs, cid);
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }


        return null;
    }

    public void storeToFile(@NonNull File file, @NonNull CID cid, @Nullable CancellationSignal signal) {
        checkNotNull(file);
        checkNotNull(cid);

        // make sure file path exists
        try {
            if (file.exists()) {
                return;
            } else {
                File parent = file.getParentFile();
                checkNotNull(parent);
                if (!parent.exists()) {
                    checkArgument(parent.mkdirs());
                }
                checkArgument(file.createNewFile());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            stream(outputStream, cid, signal);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void stream(@NonNull OutputStream outputStream, @NonNull CID cid, @Nullable CancellationSignal signal) {
        checkNotNull(outputStream);
        checkNotNull(cid);

        int blockSize = 4096;
        try {
            Reader fileReader = ipfs.getReader(cid, true);

            try {

                fileReader.readData(blockSize);

                long bytesRead = fileReader.getRead();


                while (bytesRead > 0) {

                    if (signal != null) {
                        if (signal.isCanceled()) {
                            return;
                        }
                    }

                    outputStream.write(fileReader.getData(), 0, (int) bytesRead);

                    fileReader.readData(blockSize);
                    bytesRead = fileReader.getRead();
                }
            } finally {
                fileReader.close();
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean onCreate() {

        // TODO remove this
        Service.getInstance(getContext()); //todo

        threads = THREADS.getInstance(getContext());

        ipfs = Singleton.getInstance(getContext()).getIpfs(); //todo
        return true;
    }

    private static class ParcelFileDescriptorUtil {

        public static ParcelFileDescriptor pipeFrom(IPFS ipfs, CID cid)
                throws Exception {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
            final OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

            new IPFSTransferThread(ipfs, cid, output).start();

            return pipe[0];
        }

        @SuppressWarnings("unused")
        public static ParcelFileDescriptor pipeTo(OutputStream outputStream)
                throws IOException {
            final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
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

    static class IPFSTransferThread extends java.lang.Thread {

        final OutputStream mOut;
        private final Reader reader;

        IPFSTransferThread(IPFS ipfs, CID cid, OutputStream out) throws Exception {
            super("ParcelFileDescriptor Transfer Thread");
            reader = ipfs.getReader(cid, true);

            mOut = out;
        }

        @Override
        public void run() {
            try {
                int size = 4096;
                int position = 0;
                reader.readAt(position, size);
                long read = reader.getRead();
                while (read > 0) {
                    byte[] data = reader.getData();
                    mOut.write(data, 0, data.length);
                    position += read;
                    reader.readAt(position, size);
                    read = reader.getRead();
                }
                Log.e(TAG, "success upload");
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            } finally {
                // todo make sure that reader and writer is closed
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                }
                try {
                    mOut.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                }
            }
            // todo just get input stream from ipfs and close it afterwards
            // IOUtils.copy(mIn, mOut);

        }
    }
}