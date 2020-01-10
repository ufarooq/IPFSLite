package com.j256.simplemagic.entries;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

/**
 * Loads the IANA databases (build on 10 august 2017). IANA databases provides the following elements in a CSV file:
 * <ul>
 * <li>Name of the file type</li>
 * <li>Mime-type</li>
 * <li>Name of the articles describing the mime type</li>
 * </ul>
 * In addition to these elements, two URLs are created in order to locate the description of the mime type and the URL
 * of the articles.
 *
 * @author Jean-Christophe Malapert
 */
public class IanaEntries {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("\\[(.+?)\\]");
    private final Map<String, IanaEntry> entryMap = new HashMap<String, IanaEntry>();

    public IanaEntries(@NonNull Context context) {
        loadFile(context, R.raw.iana_app);
        loadFile(context, R.raw.iana_audio);
        loadFile(context, R.raw.iana_font);
    }

    /**
     * Returns the IANA metadata for a specific mime type or null if not found.
     */
    public IanaEntry lookupByMimeType(String mimeType) {
        return entryMap.get(mimeType);
    }

    /**
     * Loads the IANA database from the specified file.
     */
    private void loadFile(@NonNull Context context, @RawRes int resource) {
        InputStream stream = context.getResources().openRawResource(resource);
        checkNotNull(stream);
        BufferedReader lineReader = null;
        try {
            lineReader = new BufferedReader(new InputStreamReader(stream));
            stream = null;
            // skip the first line header
            lineReader.readLine();
            while (true) {
                String line = lineReader.readLine();
                if (line == null) {
                    break;
                }
                // parse the CSV file. The CSV file contains three elements per row
                String[] parsed = line.split(",");
                if (parsed.length < 3) {
                    // ignore invalid entries
                    continue;
                }
                String name = parsed[0];
                String mimeType = parsed[1];
                if (mimeType.isEmpty()) {
                    continue;
                }
                String reference = parsed[2];
                // fix problem in the CSV file provided by IANA such as G719,audio/G719,"[RFC5404][RFC Errata 3245]"
                if (reference.startsWith("\"")) {
                    String nextLine = lineReader.readLine();
                    nextLine = nextLine.replaceAll("\\s+", "");
                    reference += nextLine;
                }
                IanaEntry ianaEntry = new IanaEntry(name, mimeType, parseReference(reference));
                entryMap.put(mimeType, ianaEntry);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error when loading", ioe);
        } finally {
            closeQuietly(lineReader);
            closeQuietly(stream);
        }
    }

    /**
     * Parses the references (such as RFC document) associated to a mime type. One or several references can be
     * associated to a mime type. Each reference is encompassed by this pattern [ ].
     */
    private List<String> parseReference(String reference) {
        List<String> references = new ArrayList<String>();
        Matcher matcher = REFERENCE_PATTERN.matcher(reference);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
        return references;
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
}
