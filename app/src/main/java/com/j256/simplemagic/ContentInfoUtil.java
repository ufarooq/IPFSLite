package com.j256.simplemagic;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.j256.simplemagic.entries.IanaEntries;
import com.j256.simplemagic.entries.MagicEntries;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;

import threads.server.R;


/**
 * <p>
 * Class which reads in the magic files and determines the {@link ContentInfo} for files and byte arrays. You use the
 * default constructor  to use the internal rules file or load in a local file from the
 * file-system using }. Once the rules are loaded, you can use
 * or other {@code findMatch(...)} methods to getData the content-type of a file or bytes.
 * </p>
 *
 * <pre>
 * // create a magic utility using the internal magic file
 * ContentInfoUtil util = new ContentInfoUtil();
 * // getData the content info for this file-path or null if no match
 * ContentInfo info = util.findMatch(&quot;/tmp/upload.tmp&quot;);
 * // display content type information
 * if (info == null) {
 * 	System.out.println(&quot;Unknown content-type&quot;);
 * } else {
 * 	// other information in ContentInfo type
 * 	System.out.println(&quot;Content-type is: &quot; + info.getName());
 * }
 * </pre>
 *
 * @author graywatson
 */
public class ContentInfoUtil {

    /**
     * Number of bytes that the utility class by default reads to determine the content type information.
     */
    final static int DEFAULT_READ_SIZE = 10 * 1024;
    private final static String INTERNAL_MAGIC_FILE = "/magic.gz";
    private static MagicEntries internalMagicEntries;
    private static IanaEntries ianaEntries;
    private final MagicEntries magicEntries;
    private int fileReadSize = DEFAULT_READ_SIZE;


    /**
     * Construct a magic utility using the internal magic file built into the package. This also allows the caller to
     * log any errors discovered in the file(s).
     */
    public ContentInfoUtil(@NonNull Context context) {
        ContentInfoUtil.createIanaEntries(context);
        if (internalMagicEntries == null) {
            try {
                internalMagicEntries = readEntriesFromResource(context, R.raw.magic);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Could not load entries from internal magic file: " + INTERNAL_MAGIC_FILE, e);
            }
            if (internalMagicEntries == null) {
                throw new IllegalStateException("Internal magic file not found in class-path: " + INTERNAL_MAGIC_FILE);
            }
        }
        this.magicEntries = internalMagicEntries;
    }


    /**
     * Return the content type if the extension from the file-name matches our internal list. This can either be just
     * the extension part or it will look for the last period and take the string after that as the extension.
     *
     * @return The matching content-info or null if no matches.
     */
    public static ContentInfo findExtensionMatch(String name) {
        name = name.toLowerCase();

        // look up the whole name first
        ContentType type = ContentType.fromFileExtension(name);
        if (type != ContentType.OTHER) {
            return new ContentInfo(type);
        }

        // now find the .ext part, if any
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return null;
        }

        type = ContentType.fromFileExtension(name.substring(index + 1));
        if (type == ContentType.OTHER) {
            return null;
        } else {
            return new ContentInfo(type);
        }
    }

    /**
     * Return the content type if the mime-type matches our internal list.
     *
     * @return The matching content-info or null if no matches.
     */
    public static ContentInfo findMimeTypeMatch(String mimeType) {
        ContentType type = ContentType.fromMimeType(mimeType.toLowerCase());
        if (type == ContentType.OTHER) {
            return null;
        } else {
            return new ContentInfo(type);
        }
    }

    private static void createIanaEntries(@NonNull Context context) {
        if (ianaEntries == null) {
            ianaEntries = new IanaEntries(context);
        }
    }

    public static IanaEntries getIanaEntries() {
        return ianaEntries;
    }

    /**
     * Return the content type for the file-path or null if none of the magic entries matched.
     *
     * @throws IOException If there was a problem reading from the file.
     */
    public ContentInfo findMatch(String filePath) throws IOException {
        return findMatch(new File(filePath));
    }

    /**
     * Return the content type for the file or null if none of the magic entries matched.
     *
     * @throws IOException If there was a problem reading from the file.
     */
    public ContentInfo findMatch(File file) throws IOException {
        int readSize = fileReadSize;
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file);
        }
        if (!file.canRead()) {
            throw new IOException("File is not readable: " + file);
        }
        long length = file.length();
        if (length <= 0) {
            return ContentInfo.EMPTY_INFO;
        }
        if (length < readSize) {
            readSize = (int) length;
        }
        byte[] bytes = new byte[readSize];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int numRead = fis.read(bytes);
            if (numRead <= 0) {
                return ContentInfo.EMPTY_INFO;
            }
            if (numRead < bytes.length) {
                bytes = Arrays.copyOf(bytes, numRead);
            }
        } finally {
            closeQuietly(fis);
        }
        return findMatch(bytes);
    }

    /**
     * Return the content type for the input-stream or null if none of the magic entries matched. You might want to use
     * the {@link ContentInfoInputStreamWrapper} class to delegate to an input-stream and determine content information
     * at the same time.
     *
     * <p>
     * <b>NOTE:</b> The caller is responsible for closing the input-stream.
     * </p>
     *
     * @throws IOException If there was a problem reading from the input-stream.
     * @see ContentInfoInputStreamWrapper
     */
    public ContentInfo findMatch(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[fileReadSize];
        int numRead = inputStream.read(bytes);
        if (numRead < 0) {
            return null;
        }
        if (numRead < bytes.length) {
            // move the bytes into a smaller array
            bytes = Arrays.copyOf(bytes, numRead);
        }
        return findMatch(bytes);
    }

    /**
     * Return the content type from the associated bytes or null if none of the magic entries matched.
     */
    public ContentInfo findMatch(byte[] bytes) {
        if (bytes.length == 0) {
            return ContentInfo.EMPTY_INFO;
        } else {
            return magicEntries.findMatch(bytes);
        }
    }

    /**
     * Set the default size that will be read if we are getting the content from a file.
     *
     * @see #DEFAULT_READ_SIZE
     */
    public void setFileReadSize(int fileReadSize) {
        this.fileReadSize = fileReadSize;
    }

    /**
     * @deprecated Not used since it is only passed into the constructor.
     */
    @Deprecated
    public void setErrorCallBack(ErrorCallBack errorCallBack) {
        // no op
    }

    private MagicEntries readEntriesFromFile(File fileOrDirectory)
            throws IOException {
        if (fileOrDirectory.isFile()) {
            FileReader reader = new FileReader(fileOrDirectory);
            try {
                return readEntries(reader);
            } finally {
                closeQuietly(reader);
            }
        } else if (fileOrDirectory.isDirectory()) {
            MagicEntries entries = new MagicEntries();
            for (File subFile : fileOrDirectory.listFiles()) {
                FileReader fr = new FileReader(subFile);
                try {
                    readEntries(entries, fr);
                } catch (IOException e) {
                    // ignore the file
                } finally {
                    closeQuietly(fr);
                }
            }
            entries.optimizeFirstBytes();
            return entries;
        } else {
            return null;
        }
    }

    private MagicEntries readEntriesFromResource(@NonNull Context context,
                                                 @RawRes int resource) throws IOException {

        InputStream stream = context.getResources().openRawResource(resource);

        if (stream == null) {
            return null;
        }
        Reader reader = null;
        try {
            // this suffix test is here for testing purposes so we can generate a simple magic file
            //if (resource.endsWith(".gz")) {
            reader = new InputStreamReader(new BufferedInputStream(stream));
				/*
			} else {
				reader = new InputStreamReader(new BufferedInputStream(stream));
			}*/
            stream = null;
            return readEntries(reader);
        } finally {
            closeQuietly(reader);
            closeQuietly(stream);
        }
    }

    private MagicEntries readEntries(Reader reader) throws IOException {
        MagicEntries entries = new MagicEntries();
        readEntries(entries, reader);
        entries.optimizeFirstBytes();
        return entries;
    }

    private void readEntries(MagicEntries entries, Reader reader) throws IOException {
        BufferedReader lineReader = new BufferedReader(reader);
        try {
            entries.readEntries(lineReader, null);
        } finally {
            closeQuietly(lineReader);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    /**
     * Optional call-back which will be made whenever we discover an error while parsing the magic configuration files.
     * There are usually tons of badly formed lines and other errors.
     */
    public interface ErrorCallBack {

        /**
         * An error was generated while processing the line.
         *
         * @param line    Line where the error happened.
         * @param details Specific information about the error.
         * @param e       Exception that was thrown trying to parse the line or null if none.
         */
        void error(String line, String details, Exception e);
    }
}
