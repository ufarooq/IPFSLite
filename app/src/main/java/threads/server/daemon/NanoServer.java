package threads.server.daemon;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import static com.google.common.base.Preconditions.checkNotNull;


public abstract class NanoServer {

    /**
     * Common MIME type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";
    /**
     * Common MIME type for dynamic content: html
     */
    static final String MIME_HTML = "text/html";
    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     * This is required as the Keep-Alive HTTP connections would otherwise block
     * the socket reading thread forever (or as long the browser is open).
     */
    private static final int SOCKET_READ_TIMEOUT = 5000;
    private static final String TAG = NanoServer.class.getSimpleName();
    private final Gson gson = new GsonBuilder().create();

    private final String hostname;
    private final int myPort;
    /**
     * Pluggable strategy for asynchronously executing requests.
     */
    private DefaultAsyncRunner asyncRunner;
    private volatile ServerSocket myServerSocket;
    private ServerSocketFactory serverSocketFactory = new DefaultServerSocketFactory();
    private Thread myThread;

    /**
     * Constructs an HTTP server on given port and hostname.
     */
    NanoServer(@NonNull String hostname, int port) {
        checkNotNull(hostname);
        this.hostname = hostname;
        this.myPort = port;
        setAsyncRunner(new DefaultAsyncRunner());
    }


    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an
     * array of loaded KeyManagers. These objects must properly
     * loaded/initialized by the caller.
     */
    private static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManager[] keyManagers) throws IOException {
        SSLServerSocketFactory res;
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
            res = ctx.getServerSocketFactory();
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return res;
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and a
     * loaded KeyManagerFactory. These objects must properly loaded/initialized
     * by the caller.
     */
    private static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManagerFactory loadedKeyFactory) throws IOException {
        try {
            return makeSSLSocketFactory(loadedKeyStore, loadedKeyFactory.getKeyManagers());
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a KeyStore resource with your
     * certificate and passphrase
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(String keyAndTrustStoreClasspathPath, char[] passphrase) throws IOException {
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = NanoServer.class.getResourceAsStream(keyAndTrustStoreClasspathPath);

            if (keystoreStream == null) {
                throw new IOException("Unable to load keystore from classpath: " + keyAndTrustStoreClasspathPath);
            }

            keystore.load(keystoreStream, passphrase);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            return makeSSLSocketFactory(keystore, keyManagerFactory);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }


    private static void safeClose(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    protected static Map<String, List<String>> decodeParameters(String queryString) {
        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String propertyName = sep >= 0 ? decodePercent(e.substring(0, sep)).trim() : decodePercent(e).trim();
                if (!parms.containsKey(propertyName)) {
                    parms.put(propertyName, new ArrayList<>());
                }
                String propertyValue = sep >= 0 ? decodePercent(e.substring(sep + 1)) : null;
                if (propertyValue != null) {
                    parms.get(propertyName).add(propertyValue);
                }
            }
        }
        return parms;
    }

    /**
     * Decode percent encoded <code>String</code> values.
     *
     * @param str the percent encoded <code>String</code>
     * @return expanded form of the input, for example "foo%20bar" becomes
     * "foo bar"
     */
    private static String decodePercent(String str) {
        String decoded = null;
        try {
            decoded = URLDecoder.decode(str, "UTF8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Encoding not supported, ignored", e);
        }
        return decoded;
    }


    /**
     * Create a response with known length.
     */
    static Response newFixedLengthResponse(Response.Status status, String mimeType, InputStream data, long totalBytes) {
        return new Response(status, mimeType, data, totalBytes);
    }

    /**
     * Create a text response with known length.
     */
    public static Response newFixedLengthResponse(Response.Status status, String mimeType, String txt) {
        ContentType contentType = new ContentType(mimeType);
        if (txt == null) {
            return newFixedLengthResponse(status, mimeType, new ByteArrayInputStream(new byte[0]), 0);
        } else {
            byte[] bytes;
            try {
                CharsetEncoder newEncoder = Charset.forName(contentType.getEncoding()).newEncoder();
                if (!newEncoder.canEncode(txt)) {
                    contentType = contentType.tryUTF8();
                }
                bytes = txt.getBytes(contentType.getEncoding());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "encoding problem, responding nothing", e);
                bytes = new byte[0];
            }
            return newFixedLengthResponse(status, contentType.getContentTypeHeader(), new ByteArrayInputStream(bytes), bytes.length);
        }
    }

    /**
     * Create a text response with known length.
     */
    public static Response newFixedLengthResponse(String msg) {
        return newFixedLengthResponse(Response.Status.OK, NanoServer.MIME_HTML, msg);
    }

    /**
     * Forcibly closes all connections that are open.
     */
    public synchronized void closeAllConnections() {
        shutdown();
    }

    /**
     * create a instance of the client handler, subclasses can return a subclass
     * of the ClientHandler.
     *
     * @param finalAccept the socket the cleint is connected to
     * @param inputStream the input stream
     * @return the client handler
     */
    protected ClientHandler createClientHandler(final Socket finalAccept, final InputStream inputStream) {
        return new ClientHandler(inputStream, finalAccept);
    }

    /**
     * Instantiate the server runnable, can be overwritten by subclasses to
     * provide a subclass of the ServerRunnable.
     *
     * @param timeout the socket timeout to use.
     * @return the server runnable.
     */
    protected ServerRunnable createServerRunnable(final int timeout) {
        return new ServerRunnable(timeout);
    }


    public final boolean isAlive() {
        return wasStarted() && !this.myServerSocket.isClosed() && this.myThread.isAlive();
    }

    public ServerSocketFactory getServerSocketFactory() {
        return serverSocketFactory;
    }

    public void setServerSocketFactory(ServerSocketFactory serverSocketFactory) {
        this.serverSocketFactory = serverSocketFactory;
    }


    /**
     * Call before start() to serve over HTTPS instead of HTTP
     */
    public void makeSecure(SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
        this.serverSocketFactory = new SecureServerSocketFactory(sslServerSocketFactory, sslProtocols);
    }


    public abstract Response serve(IHTTPSession session);

    // -------------------------------------------------------------------------------
    // //
    //
    // Threading Strategy.
    //
    // -------------------------------------------------------------------------------
    // //


    /**
     * Pluggable strategy for asynchronously executing requests.
     *
     * @param asyncRunner new strategy for handling threads.
     */
    private void setAsyncRunner(DefaultAsyncRunner asyncRunner) {
        this.asyncRunner = asyncRunner;
    }

    /**
     * Start the server.
     *
     * @throws IOException if the socket is in use.
     */
    public void start() throws Exception {
        start(NanoServer.SOCKET_READ_TIMEOUT);
    }

    /**
     * Starts the server (in setDaemon(true) mode).
     */
    public void start(final int timeout) throws Exception {
        start(timeout, true);
    }

    /**
     * Start the server.
     *
     * @param timeout timeout to use for socket connections.
     * @param daemon  start the thread daemon or not.
     * @throws IOException if the socket is in use.
     */
    public void start(final int timeout, boolean daemon) throws Exception {
        this.myServerSocket = this.getServerSocketFactory().create();
        this.myServerSocket.setReuseAddress(true);

        ServerRunnable serverRunnable = createServerRunnable(timeout);
        this.myThread = new Thread(serverRunnable);
        this.myThread.setDaemon(daemon);
        this.myThread.setName("Main Listener");
        this.myThread.start();
        while (!serverRunnable.hasBinded && serverRunnable.bindException == null) {
            try {
                Thread.sleep(10L);
            } catch (Throwable e) {
                // on android this may not be allowed, that's why we
                // catch throwable the wait should be very short because we are
                // just waiting for the bind of the socket
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        if (serverRunnable.bindException != null) {
            throw new RuntimeException(serverRunnable.bindException);
        }
    }

    // -------------------------------------------------------------------------------
    // //

    /**
     * Stop the server.
     */
    public void shutdown() {
        try {
            safeClose(this.myServerSocket);
            this.asyncRunner.closeAll();
            if (this.myThread != null) {
                this.myThread.join();
            }
        } catch (Throwable e) {
            Log.e(TAG, "Could not abort all connections", e);
        }
    }

    private boolean wasStarted() {
        return this.myServerSocket != null && this.myThread != null;
    }

    /**
     * HTTP Request methods, with the ability to decode a <code>String</code>
     * back to its enum value.
     */
    public enum Method {
        GET,
        PUT,
        POST,
        DELETE,
        HEAD,
        OPTIONS,
        TRACE,
        CONNECT,
        PATCH,
        PROPFIND,
        PROPPATCH,
        MKCOL,
        MOVE,
        COPY,
        LOCK,
        UNLOCK;

        static Method lookup(String method) {
            if (method == null)
                return null;

            try {
                return valueOf(method);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                return null;

            }
        }
    }


    /**
     * Handles one session, i.e. parses the HTTP markRequested and returns the
     * response.
     */
    public interface IHTTPSession {

        void execute() throws IOException;


        Map<String, String> getHeaders();

        InputStream getInputStream();

        Method getMethod();

        Map<String, String> getParms();

        Map<String, List<String>> getParameters();

        String getQueryParameterString();

        /**
         * @return the path part of the URL.
         */
        String getUri();

        /**
         * Adds the files in the markRequested body to the files map.
         */
        Map<String, Object> parseBody() throws Exception;

        /**
         * Get the remote ip address of the requester.
         *
         * @return the IP address.
         */
        String getRemoteIpAddress();

        /**
         * Get the remote hostname of the requester.
         *
         * @return the hostname.
         */
        String getRemoteHostName();
    }


    public interface ServerSocketFactory {
        ServerSocket create() throws IOException;
    }


    public static class DefaultAsyncRunner {

        private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList<>());
        private AtomicLong requestCounter = new AtomicLong(0);

        /**
         * @return a list with currently running clients.
         */
        public List<ClientHandler> getRunning() {
            return running;
        }


        void closeAll() {
            // copy of the list for concurrency
            for (ClientHandler clientHandler : new ArrayList<ClientHandler>(this.running)) {
                requestCounter.decrementAndGet();
                clientHandler.close();
            }
        }


        void closed(ClientHandler clientHandler) {
            requestCounter.decrementAndGet();
            this.running.remove(clientHandler);
        }


        void exec(ClientHandler clientHandler) {
            long req = requestCounter.getAndIncrement();
            Thread t = new Thread(clientHandler);
            t.setDaemon(true);
            t.setName("Request Processor (#" + req + ")");
            this.running.add(clientHandler);
            t.start();
        }
    }


    /**
     * Creates a normal ServerSocket for TCP connections
     */
    public static class DefaultServerSocketFactory implements ServerSocketFactory {

        @Override
        public ServerSocket create() throws IOException {
            return new ServerSocket();
        }

    }

    /**
     * Creates a new SSLServerSocket
     */
    public static class SecureServerSocketFactory implements ServerSocketFactory {

        private SSLServerSocketFactory sslServerSocketFactory;

        private String[] sslProtocols;

        SecureServerSocketFactory(SSLServerSocketFactory sslServerSocketFactory, String[] sslProtocols) {
            this.sslServerSocketFactory = sslServerSocketFactory;
            this.sslProtocols = sslProtocols;
        }

        @Override
        public ServerSocket create() throws IOException {
            SSLServerSocket ss = null;
            ss = (SSLServerSocket) this.sslServerSocketFactory.createServerSocket();
            if (this.sslProtocols != null) {
                ss.setEnabledProtocols(this.sslProtocols);
            } else {
                ss.setEnabledProtocols(ss.getSupportedProtocols());
            }
            ss.setUseClientMode(false);
            ss.setWantClientAuth(false);
            ss.setNeedClientAuth(false);
            return ss;
        }

    }

    protected static class ContentType {

        private static final String ASCII_ENCODING = "US-ASCII";

        private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";

        private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";

        private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);

        private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

        private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

        private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

        private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);

        private final String contentTypeHeader;

        private final String contentType;

        private final String encoding;

        private final String boundary;

        ContentType(String contentTypeHeader) {
            this.contentTypeHeader = contentTypeHeader;
            if (contentTypeHeader != null) {
                contentType = getDetailFromContentHeader(contentTypeHeader, MIME_PATTERN, "", 1);
                encoding = getDetailFromContentHeader(contentTypeHeader, CHARSET_PATTERN, null, 2);
            } else {
                contentType = "";
                encoding = "UTF-8";
            }
            if (MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(contentType)) {
                boundary = getDetailFromContentHeader(contentTypeHeader, BOUNDARY_PATTERN, null, 2);
            } else {
                boundary = null;
            }
        }

        private String getDetailFromContentHeader(String contentTypeHeader, Pattern pattern, String defaultValue, int group) {
            Matcher matcher = pattern.matcher(contentTypeHeader);
            return matcher.find() ? matcher.group(group) : defaultValue;
        }

        String getContentTypeHeader() {
            return contentTypeHeader;
        }

        public String getContentType() {
            return contentType;
        }

        public String getEncoding() {
            return encoding == null ? ASCII_ENCODING : encoding;
        }

        public String getBoundary() {
            return boundary;
        }

        public boolean isMultipart() {
            return MULTIPART_FORM_DATA_HEADER.equalsIgnoreCase(contentType);
        }

        ContentType tryUTF8() {
            if (encoding == null) {
                return new ContentType(this.contentTypeHeader + "; charset=UTF-8");
            }
            return this;
        }
    }

    /**
     * HTTP response. Return one of these from serve().
     */
    public static class Response implements Closeable {

        /**
         * copy of the header map with all the keys lowercase for faster
         * searching.
         */
        private final Map<String, String> lowerCaseHeader = new HashMap<String, String>();
        /**
         * Headers for the HTTP response. Use addHeader() to add lines. the
         * lowercase map is automatically kept up to date.
         */
        @SuppressWarnings("serial")
        private final Map<String, String> header = new HashMap<String, String>() {

            public String put(String key, String value) {
                lowerCaseHeader.put(key == null ? key : key.toLowerCase(), value);
                return super.put(key, value);
            }
        };
        /**
         * HTTP status code after processing, e.g. "200 OK", Status.OK
         */
        private Status status;
        /**
         * MIME type of content, e.g. "text/html"
         */
        private String mimeType;
        /**
         * UploadTask of the response, may be null.
         */
        private InputStream data;
        private long contentLength;
        /**
         * The markRequested method that spawned this response.
         */
        private Method requestMethod;

        private boolean keepAlive;


        protected Response(Status status, String mimeType, InputStream data, long totalBytes) {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
            this.contentLength = totalBytes;
            this.keepAlive = true;
        }

        @Override
        public void close() throws IOException {
            if (this.data != null) {
                this.data.close();
            }
        }

        /**
         * Adds given line to the header.
         */
        public void addHeader(String name, String value) {
            this.header.put(name, value);
        }

        /**
         * Indicate to shutdown the connection after the Response has been sent.
         *
         * @param close {@code true} to hint connection closing, {@code false} to
         *              let connection be closed by client.
         */
        public void closeConnection(boolean close) {
            if (close)
                this.header.put("connection", "shutdown");
            else
                this.header.remove("connection");
        }

        /**
         * @return {@code true} if connection is to be closed after this
         * Response has been sent.
         */
        boolean isCloseConnection() {
            return "shutdown".equals(getHeader("connection"));
        }

        public InputStream getData() {
            return this.data;
        }

        public void setData(InputStream data) {
            this.data = data;
        }

        private String getHeader(String name) {
            return this.lowerCaseHeader.get(name.toLowerCase());
        }

        public String getMimeType() {
            return this.mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public Method getRequestMethod() {
            return this.requestMethod;
        }

        void setRequestMethod(Method requestMethod) {
            this.requestMethod = requestMethod;
        }

        public Status getStatus() {
            return this.status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }


        void setKeepAlive(boolean useKeepAlive) {
            this.keepAlive = useKeepAlive;
        }

        /**
         * Sends given response to the socket.
         */
        protected void send(OutputStream outputStream) throws IOException {
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));


            if (this.status == null) {
                throw new Error("sendResponse(): Status can't be null.");
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, new ContentType(this.mimeType).getEncoding())), false);
            pw.append("HTTP/1.1 ").append(this.status.getDescription()).append(" \r\n");
            if (this.mimeType != null) {
                printHeader(pw, "Content-Type", this.mimeType);
            }
            if (getHeader("date") == null) {
                printHeader(pw, "Date", gmtFrmt.format(new Date()));
            }
            for (Entry<String, String> entry : this.header.entrySet()) {
                printHeader(pw, entry.getKey(), entry.getValue());
            }
            if (getHeader("connection") == null) {
                printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "shutdown"));
            }


            long pending = this.data != null ? this.contentLength : 0;
            pending = sendContentLengthHeaderIfNotAlreadyPresent(pw, pending);

            pw.append("\r\n");
            pw.flush();
            sendBody(outputStream, pending);
            outputStream.flush();
            safeClose(this.data);

        }

        void printHeader(PrintWriter pw, String key, String value) {
            pw.append(key).append(": ").append(value).append("\r\n");
        }

        long sendContentLengthHeaderIfNotAlreadyPresent(PrintWriter pw, long defaultSize) {
            String contentLengthString = getHeader("content-length");
            long size = defaultSize;
            if (contentLengthString != null) {
                try {
                    size = Long.parseLong(contentLengthString);
                } catch (NumberFormatException ex) {
                    Log.e(TAG, "content-length was no number " + contentLengthString);
                }
            }
            pw.print("Content-Length: " + size + "\r\n");
            return size;
        }


        /**
         * Sends the body to the specified OutputStream. The pending parameter
         * limits the maximum amounts of bytes sent unless it is -1, in which
         * case everything is sent.
         *
         * @param outputStream the OutputStream to markRequested data to
         * @param pending      -1 to markRequested everything, otherwise sets a max limit to the
         *                     number of bytes sent
         * @throws IOException if something goes wrong while sending the data.
         */
        private void sendBody(OutputStream outputStream, long pending) throws IOException {

            long BUFFER_SIZE = 16 * 1024;
            byte[] buff = new byte[(int) BUFFER_SIZE];
            boolean sendEverything = pending == -1;
            while (pending > 0 || sendEverything) {
                long bytesToRead = sendEverything ? BUFFER_SIZE : Math.min(pending, BUFFER_SIZE);
                int read = this.data.read(buff, 0, (int) bytesToRead);
                if (read <= 0) {
                    break;
                }
                outputStream.write(buff, 0, read);
                if (!sendEverything) {
                    pending -= read;
                }
            }

        }

        /**
         * Some HTTP response status codes
         */
        public enum Status {
            SWITCH_PROTOCOL(101, "Switching Protocols"),

            OK(200, "OK"),
            CREATED(201, "Created"),
            ACCEPTED(202, "Accepted"),
            NO_CONTENT(204, "No Content"),
            PARTIAL_CONTENT(206, "Partial Content"),
            MULTI_STATUS(207, "Multi-Status"),

            REDIRECT(301, "Moved Permanently"),
            /**
             * Many user agents mishandle 302 in ways that violate the RFC1945
             * spec (i.e., redirect a POST to a GET). 303 and 307 were added in
             * RFC2616 to address this. You should prefer 303 and 307 unless the
             * calling user agent does not support 303 and 307 functionality
             */
            @Deprecated
            FOUND(302, "Found"),
            REDIRECT_SEE_OTHER(303, "See Other"),
            NOT_MODIFIED(304, "Not Modified"),
            TEMPORARY_REDIRECT(307, "Temporary Redirect"),

            BAD_REQUEST(400, "Bad Request"),
            UNAUTHORIZED(401, "Unauthorized"),
            FORBIDDEN(403, "Forbidden"),
            NOT_FOUND(404, "Not Found"),
            METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
            NOT_ACCEPTABLE(406, "Not Acceptable"),
            REQUEST_TIMEOUT(408, "Request Timeout"),
            CONFLICT(409, "Conflict"),
            GONE(410, "Gone"),
            LENGTH_REQUIRED(411, "Length Required"),
            PRECONDITION_FAILED(412, "Precondition Failed"),
            PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
            UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
            RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
            EXPECTATION_FAILED(417, "Expectation Failed"),
            TOO_MANY_REQUESTS(429, "Too Many Requests"),

            INTERNAL_ERROR(500, "Internal Server Error"),
            NOT_IMPLEMENTED(501, "Not Implemented"),
            SERVICE_UNAVAILABLE(503, "Service Unavailable"),
            UNSUPPORTED_HTTP_VERSION(505, "HTTP Version Not Supported");

            private final int requestStatus;

            private final String description;

            Status(int requestStatus, String description) {
                this.requestStatus = requestStatus;
                this.description = description;
            }

            public static Status lookup(int requestStatus) {
                for (Status status : Status.values()) {
                    if (status.getRequestStatus() == requestStatus) {
                        return status;
                    }
                }
                return null;
            }


            public String getDescription() {
                return "" + this.requestStatus + " " + this.description;
            }


            public int getRequestStatus() {
                return this.requestStatus;
            }

        }


    }

    public static final class ResponseException extends Exception {

        private static final long serialVersionUID = 6569838532917408380L;

        private final Response.Status status;

        ResponseException(Response.Status status, String message) {
            super(message);
            this.status = status;
        }

        ResponseException(Response.Status status, String message, Exception e) {
            super(message, e);
            this.status = status;
        }

        public Response.Status getStatus() {
            return this.status;
        }
    }

    /**
     * The runnable that will be used for every new client connection.
     */
    public class ClientHandler implements Runnable {

        private final InputStream inputStream;

        private final Socket acceptSocket;

        ClientHandler(InputStream inputStream, Socket acceptSocket) {
            this.inputStream = inputStream;
            this.acceptSocket = acceptSocket;
        }

        public void close() {
            safeClose(this.inputStream);
            safeClose(this.acceptSocket);
        }

        @Override
        public void run() {
            OutputStream outputStream = null;
            try {
                outputStream = this.acceptSocket.getOutputStream();
                HTTPSession session = new HTTPSession(this.inputStream, outputStream, this.acceptSocket.getInetAddress());
                while (!this.acceptSocket.isClosed()) {
                    session.execute();
                }
            } catch (IOException e) {
                // DownloadClient failed for some reasons
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e); // not expected exception
            } finally {
                safeClose(outputStream);
                safeClose(this.inputStream);
                safeClose(this.acceptSocket);
                NanoServer.this.asyncRunner.closed(this);
            }
        }
    }


    protected class HTTPSession implements IHTTPSession {

        static final int BUFSIZE = 8192;

        private final OutputStream outputStream;

        private final BufferedInputStream inputStream;

        private int splitbyte;

        private int rlen;

        private String uri;

        private Method method;

        private Map<String, List<String>> parms;

        private Map<String, String> headers;


        private String queryParameterString;

        private String remoteIp;

        private String remoteHostname;

        private String protocolVersion;


        HTTPSession(InputStream inputStream, OutputStream outputStream, InetAddress inetAddress) {

            this.inputStream = new BufferedInputStream(inputStream, HTTPSession.BUFSIZE);
            this.outputStream = outputStream;
            this.remoteIp = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress();
            this.remoteHostname = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "localhost" : inetAddress.getHostName();
            this.headers = new HashMap<>();
        }

        /**
         * Decodes the sent headers and loads the data into Key/value pairs
         */
        private void decodeHeader(BufferedReader in, Map<String, String> pre, Map<String, List<String>> parms, Map<String, String> headers) throws ResponseException {
            try {
                // Read the markRequested line
                String inLine = in.readLine();
                if (inLine == null) {
                    return;
                }

                StringTokenizer st = new StringTokenizer(inLine);
                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }

                pre.put("method", st.nextToken());

                if (!st.hasMoreTokens()) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }

                String uri = st.nextToken();

                // Decode parameters from the URI
                int qmi = uri.indexOf('?');
                if (qmi >= 0) {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                } else {
                    uri = decodePercent(uri);
                }

                // If there's another token, its protocol version,
                // followed by HTTP headers.
                // NOTE: this now forces header names lower case since they are
                // case insensitive and vary by client.
                if (st.hasMoreTokens()) {
                    protocolVersion = st.nextToken();
                } else {
                    protocolVersion = "HTTP/1.1";
                    Log.i(TAG, "no protocol version specified, strange. Assuming HTTP/1.1.");
                }
                String line = in.readLine();
                while (line != null && !line.trim().isEmpty()) {
                    int p = line.indexOf(':');
                    if (p >= 0) {
                        headers.put(line.substring(0, p).trim().toLowerCase(Locale.US), line.substring(p + 1).trim());
                    }
                    line = in.readLine();
                }

                pre.put("uri", uri);
            } catch (IOException ioe) {
                throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage(), ioe);
            }
        }


        /**
         * Decodes parameters in percent-encoded URI-format ( e.g.
         * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
         * Map.
         */
        private void decodeParms(String parms, Map<String, List<String>> p) {
            if (parms == null) {
                this.queryParameterString = "";
                return;
            }

            this.queryParameterString = parms;
            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens()) {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                String key;
                String value;

                if (sep >= 0) {
                    key = decodePercent(e.substring(0, sep)).trim();
                    value = decodePercent(e.substring(sep + 1));
                } else {
                    key = decodePercent(e).trim();
                    value = "";
                }

                List<String> values = p.get(key);
                if (values == null) {
                    values = new ArrayList<>();
                    p.put(key, values);
                }

                values.add(value);
            }
        }


        @Override
        public void execute() throws IOException {
            Response r = null;
            try {

                // Read the first 8192 bytes.
                // The full header should fit in here.
                // Apache's default header limit is 8KB.
                // Do NOT assume that a single read will get the entire header
                // at once!
                byte[] buf = new byte[HTTPSession.BUFSIZE];
                this.splitbyte = 0;
                this.rlen = 0;

                int read = -1;
                this.inputStream.mark(HTTPSession.BUFSIZE);
                try {
                    read = this.inputStream.read(buf, 0, HTTPSession.BUFSIZE);
                } catch (IOException e) {
                    safeClose(this.inputStream);
                    safeClose(this.outputStream);
                    throw new SocketException();
                }
                if (read == -1) {
                    // socket was been closed
                    safeClose(this.inputStream);
                    safeClose(this.outputStream);
                    throw new SocketException();
                }
                while (read > 0) {
                    this.rlen += read;
                    this.splitbyte = findHeaderEnd(buf, this.rlen);
                    if (this.splitbyte > 0) {
                        break;
                    }
                    read = this.inputStream.read(buf, this.rlen, HTTPSession.BUFSIZE - this.rlen);
                }

                if (this.splitbyte < this.rlen) {
                    this.inputStream.reset();
                    this.inputStream.skip(this.splitbyte);
                }

                this.parms = new HashMap<String, List<String>>();
                if (null == this.headers) {
                    this.headers = new HashMap<String, String>();
                } else {
                    this.headers.clear();
                }

                // Create a BufferedReader for parsing the header.
                BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, this.rlen)));

                // Decode the header into parms and header java properties
                Map<String, String> pre = new HashMap<>();
                decodeHeader(hin, pre, this.parms, this.headers);

                if (null != this.remoteIp) {
                    this.headers.put("remote-addr", this.remoteIp);
                    this.headers.put("http-client-ip", this.remoteIp);
                }

                this.method = Method.lookup(pre.get("method"));
                if (this.method == null) {
                    throw new ResponseException(Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. HTTP verb " + pre.get("method") + " unhandled.");
                }

                this.uri = pre.get("uri");


                String connection = this.headers.get("connection");
                boolean keepAlive = "HTTP/1.1".equals(protocolVersion) && (connection == null || !connection.matches("(?i).*close.*"));

                // Ok, now do the serve()

                // TODO: long body_size = getBodySize();
                // TODO: long pos_before_serve = this.inputStream.totalRead()
                // (requires implementation for totalRead())
                r = serve(this);
                // TODO: this.inputStream.skip(body_size -
                // (this.inputStream.totalRead() - pos_before_serve))

                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    String acceptEncoding = this.headers.get("accept-encoding");

                    r.setRequestMethod(this.method);
                    r.setKeepAlive(keepAlive);
                    r.send(this.outputStream);
                }
                if (!keepAlive || r.isCloseConnection()) {
                    throw new SocketException();
                }
            } catch (ResponseException re) {
                Response resp = newFixedLengthResponse(re.getStatus(), NanoServer.MIME_PLAINTEXT, re.getMessage());
                resp.send(this.outputStream);
                safeClose(this.outputStream);
            } finally {
                safeClose(r);
            }
        }

        /**
         * Find byte index separating header from body. It must be the last byte
         * of the first two sequential new lines.
         */
        private int findHeaderEnd(final byte[] buf, int rlen) {
            int splitbyte = 0;
            while (splitbyte + 1 < rlen) {

                // RFC2616
                if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && splitbyte + 3 < rlen && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                    return splitbyte + 4;
                }

                // tolerance
                if (buf[splitbyte] == '\n' && buf[splitbyte + 1] == '\n') {
                    return splitbyte + 2;
                }
                splitbyte++;
            }
            return 0;
        }

        /**
         * Find the byte positions where multipart boundaries start. This reads
         * a large block at a time and uses a temporary buffer to optimize
         * (memory mapped) file access.
         */
        private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
            int[] res = new int[0];
            if (b.remaining() < boundary.length) {
                return res;
            }

            int search_window_pos = 0;
            byte[] search_window = new byte[4 * 1024 + boundary.length];

            int first_fill = (b.remaining() < search_window.length) ? b.remaining() : search_window.length;
            b.get(search_window, 0, first_fill);
            int new_bytes = first_fill - boundary.length;

            do {
                // Search the search_window
                for (int j = 0; j < new_bytes; j++) {
                    for (int i = 0; i < boundary.length; i++) {
                        if (search_window[j + i] != boundary[i])
                            break;
                        if (i == boundary.length - 1) {
                            // Match found, add it to results
                            int[] new_res = new int[res.length + 1];
                            System.arraycopy(res, 0, new_res, 0, res.length);
                            new_res[res.length] = search_window_pos + j;
                            res = new_res;
                        }
                    }
                }
                search_window_pos += new_bytes;

                // Copy the end of the buffer to the start
                System.arraycopy(search_window, search_window.length - boundary.length, search_window, 0, boundary.length);

                // Refill search_window
                new_bytes = search_window.length - boundary.length;
                new_bytes = (b.remaining() < new_bytes) ? b.remaining() : new_bytes;
                b.get(search_window, boundary.length, new_bytes);
            } while (new_bytes > 0);
            return res;
        }


        @Override
        public final Map<String, String> getHeaders() {
            return this.headers;
        }

        @Override
        public final InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public final Method getMethod() {
            return this.method;
        }

        @Override
        public final Map<String, String> getParms() {
            Map<String, String> result = new HashMap<>();
            for (String key : this.parms.keySet()) {
                result.put(key, this.parms.get(key).get(0));
            }

            return result;
        }

        @Override
        public final Map<String, List<String>> getParameters() {
            return this.parms;
        }

        @Override
        public String getQueryParameterString() {
            return this.queryParameterString;
        }


        @Override
        public final String getUri() {
            return this.uri;
        }

        /**
         * Deduce body length in bytes. Either from "content-length" header or
         * read bytes.
         */
        long getBodySize() {
            if (this.headers.containsKey("content-length")) {
                return Long.parseLong(this.headers.get("content-length"));
            } else if (this.splitbyte < this.rlen) {
                return this.rlen - this.splitbyte;
            }
            return 0;
        }


        public Map<String, Object> parseBody() throws Exception {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int size = (int) getBodySize();
            byte[] buf = new byte[512];
            int rlen = 0;
            while (rlen >= 0 && size > 0) {
                rlen = getInputStream().read(buf, 0, Math.min(size, 512));
                size -= rlen;
                if (rlen > 0) {
                    baos.write(buf, 0, rlen);
                }
            }


            // If the method is POST, there may be parameters
            // in data section, too, read it:
            String result = baos.toString();
            baos.close();
            if (!result.isEmpty()) {
                if (Method.POST.equals(getMethod())) {
                    return gson.fromJson(result, Map.class);
                }
                if (Method.PUT.equals(getMethod())) {
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("data", result);
                    return hashMap;
                }
            }

            return new HashMap<>();

        }

        @Override
        public String getRemoteIpAddress() {
            return this.remoteIp;
        }

        @Override
        public String getRemoteHostName() {
            return this.remoteHostname;
        }
    }


    /**
     * The runnable that will be used for the main listening thread.
     */
    public class ServerRunnable implements Runnable {

        private final int timeout;

        private Throwable bindException;

        private boolean hasBinded = false;

        ServerRunnable(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                if (hostname.isEmpty()) {
                    myServerSocket.bind(new InetSocketAddress(myPort));
                } else {
                    myServerSocket.bind(new InetSocketAddress(hostname, myPort));
                }
                hasBinded = true;
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                this.bindException = e;
                return;
            }
            do {
                try {
                    final Socket finalAccept = NanoServer.this.myServerSocket.accept();
                    if (this.timeout > 0) {
                        finalAccept.setSoTimeout(this.timeout);
                    }
                    final InputStream inputStream = finalAccept.getInputStream();
                    NanoServer.this.asyncRunner.exec(createClientHandler(finalAccept, inputStream));
                } catch (SSLHandshakeException e) {
                    // This kind of exception will be ignored, due to wrong SSL handshake
                } catch (Throwable e) {
                    if (e instanceof SocketException && myServerSocket.isClosed()) {
                        Log.i(TAG, "Closing server connection");
                    } else {
                        Log.e(TAG, "Communication with the client broken", e);
                    }
                }
            } while (!NanoServer.this.myServerSocket.isClosed());
        }
    }
}
