package threads.server.daemon;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import threads.iota.IotaAPI;
import threads.iota.IotaClient;
import threads.iota.TangleUtils;
import threads.iota.dto.response.FindTransactionResponse;
import threads.iota.dto.response.GetAttachToTangleResponse;
import threads.iota.event.EventsDatabase;
import threads.iota.model.Hash;
import threads.iota.server.IServer;
import threads.iota.utils.Converter;
import threads.server.daemon.dto.AbstractResponse;
import threads.server.daemon.dto.AccessLimitedResponse;
import threads.server.daemon.dto.AttachToTangleResponse;
import threads.server.daemon.dto.ErrorResponse;
import threads.server.daemon.dto.ExceptionResponse;
import threads.server.daemon.dto.FindTransactionsResponse;
import threads.server.daemon.dto.GetNodeInfoResponse;
import threads.server.daemon.dto.GetTipsResponse;
import threads.server.daemon.dto.GetTransactionsToApproveResponse;
import threads.server.daemon.dto.GetTrytesResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public class WebServer extends NanoServer implements IServer {

    private static final String TAG = WebServer.class.getSimpleName();
    private final static int HASH_SIZE = 81;
    private final static int TRYTES_SIZE = 2673;
    private final String authentification;
    private final Gson gson = new GsonBuilder().create();
    private final TransactionDatabase transactionDatabase;
    private final IotaAPI iotaAPI;
    private final EventsDatabase eventsDatabase;
    private final HashSet<String> remoteHosts = new HashSet<>();
    int DEFAULT_PIECE_LENGTH = 512 * 1024;

    private WebServer(@NonNull String hostname,
                      @NonNull Integer port,
                      @NonNull TransactionDatabase transactionDatabase,
                      @NonNull EventsDatabase eventsDatabase,
                      @NonNull String authentification) {
        super(hostname, port);
        checkNotNull(transactionDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(authentification);
        this.eventsDatabase = eventsDatabase;
        this.transactionDatabase = transactionDatabase;
        this.authentification = authentification;

        IotaAPI.Builder builder = new IotaAPI.Builder();
        builder = builder.protocol("https");
        builder = builder.host("nodes.thetangle.org");
        builder = builder.port("443");
        iotaAPI = builder.build();


    }

    /**
     * Create a text response with known length.
     */
    private static Response newFixedErrorLengthResponse(String msg) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoServer.MIME_HTML, msg);
    }

    public static WebServer getInstance(@NonNull String hostname,
                                        @NonNull Integer port,
                                        @NonNull TransactionDatabase transactionDatabase,
                                        @NonNull EventsDatabase eventsDatabase,
                                        @NonNull String authentification) {
        checkNotNull(hostname);
        checkNotNull(port);
        checkNotNull(transactionDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(authentification);
        return new WebServer(hostname, port, transactionDatabase, eventsDatabase, authentification);
    }

    private static void setupResponseHeaders(final NanoServer.IHTTPSession exchange) {
        Map<String, String> headerMap = exchange.getHeaders();
        headerMap.put("Access-Control-Allow-Origin", "*");
        headerMap.put("Keep-Alive", "timeout=500, max=100");
    }

    @Override
    public Response serve(IHTTPSession session) {
        return processRequest(session);
    }

    private Response processRequest(final IHTTPSession session) {
        final long timeMillis = System.currentTimeMillis();

        final AbstractResponse response;

        try {
            session.getHeaders().put("Content-Type", "application/json");

            Map<String, Object> map = session.parseBody();
            map.putAll(session.getParms());

            String remoteHost = session.getRemoteHostName();

            if (!remoteHosts.contains(remoteHost)) {
                eventsDatabase.insertMessage("Remote host " + remoteHost
                        + " [" + session.getRemoteIpAddress() + "]" + " connected...");
                remoteHosts.add(remoteHost);
            }


            if (!authentification.isEmpty()) {
                String remoteAuthentification = session.getHeaders().get(
                        IThreadsServer.AUTHENTIFICATION.toLowerCase());
                if (remoteAuthentification == null || remoteAuthentification.isEmpty()) {
                    eventsDatabase.insertMessage("Authentification required ...");
                    return newFixedErrorLengthResponse("Authentification required");
                }
                if (!remoteAuthentification.equals(authentification)) {
                    eventsDatabase.insertMessage("Authentification failure ...");
                    return newFixedErrorLengthResponse("Authentification failure");
                }
            }

            response = process(map, timeMillis);

            return sendResponse(session, response, timeMillis);


        } catch (Throwable e) {
            if (isAlive()) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                eventsDatabase.insertMessage("Internal Error : " + e.getLocalizedMessage());
                return newFixedErrorLengthResponse("Internal Error : " + e.getLocalizedMessage());
            } else {
                return newFixedErrorLengthResponse("Server is going down.");
            }
        }

    }


    private String getParameterAsString(Map<String, Object> request, String paramName) {
        validateParamExists(request, paramName);
        return (String) request.get(paramName);
    }

    private void validateParamExists(Map<String, Object> request, String paramName) {
        if (!request.containsKey(paramName)) {
            throw new RuntimeException("Missing parameter : " + paramName);
        }
    }

    private int getParameterAsInt(Map<String, Object> request, String paramName) {
        validateParamExists(request, paramName);
        final int result;
        try {
            result = ((Double) request.get(paramName)).intValue();
        } catch (ClassCastException e) {
            throw new RuntimeException("Invalid " + paramName + " input");
        }
        return result;
    }

    private List<String> getParameterAsList(Map<String, Object> request, String paramName, int size) {
        validateParamExists(request, paramName);
        final List<String> paramList = (List<String>) request.get(paramName);

        return paramList;

    }


    private AbstractResponse process(@NonNull final Map<String, Object> request, long timeMillis) {

        checkNotNull(request);
        try {


            final String command = (String) request.get("command");
            if (command == null) {
                return ErrorResponse.create("COMMAND parameter has not been specified in the markRequested.");
            }


            switch (command) {
                case "getNodeInfo": {
                    threads.iota.dto.response.GetNodeInfoResponse res = iotaAPI.getNodeInfo();
                    checkNotNull(res);


                    return GetNodeInfoResponse.create(
                            res.getAppName(),
                            res.getAppVersion(),
                            Runtime.getRuntime().availableProcessors(),
                            Runtime.getRuntime().freeMemory(),
                            System.getProperty("java.version"),
                            Runtime.getRuntime().maxMemory(),
                            Runtime.getRuntime().totalMemory(),
                            Hash.convertToHash(res.getLatestMilestone()),
                            res.getLatestMilestoneIndex(),
                            Hash.convertToHash(res.getLatestSolidSubtangleMilestone()),
                            res.getLatestSolidSubtangleMilestoneIndex(),
                            res.getLatestMilestoneIndex(), // TODO might be wrong
                            res.getNeighbors(),
                            res.getPacketsQueueSize(),
                            res.getTime(),
                            res.getTips(),
                            res.getTransactionsToRequest(),
                            true);
                }

                case "attachToTangle": {
                    final Hash trunkTransaction = new Hash(getParameterAsString(request, "trunkTransaction"));
                    final Hash branchTransaction = new Hash(getParameterAsString(request, "branchTransaction"));
                    final int minWeightMagnitude = getParameterAsInt(request, "minWeightMagnitude");

                    final List<String> trytes = getParameterAsList(request, "trytes", TRYTES_SIZE);

                    try {
                        GetAttachToTangleResponse res = TangleUtils.localPow(trunkTransaction.toString(),
                                branchTransaction.toString(),
                                minWeightMagnitude,
                                Iterables.toArray(trytes, String.class));

                        return AttachToTangleResponse.create(Arrays.asList(res.getTrytes()));
                    } finally {

                        String sb = "AttachToTangle consumed "
                                + ((System.currentTimeMillis() - timeMillis) / 1000)
                                + " [s] processing time.";
                        Log.d(TAG, sb);
                        eventsDatabase.insertMessage(sb);

                    }
                }
                case "broadcastTransactions": {
                    final List<String> trytes = getParameterAsList(request, "trytes", TRYTES_SIZE);

                    iotaAPI.broadcastTransactions(Iterables.toArray(trytes, String.class));

                    return AbstractResponse.createEmptyResponse();
                }
                case "findTransactions": {

                    String[] bundlesArray = null;
                    String[] addressesArray = null;
                    String[] approvesArray = null;
                    String[] tagsArray = null;
                    if (request.containsKey("addresses")) {
                        final List<String> bundles = (List<String>) request.get("addresses");
                        addressesArray = Iterables.toArray(bundles, String.class);
                    }
                    if (request.containsKey("tags")) {
                        final List<String> bundles = (List<String>) request.get("tags");
                        tagsArray = Iterables.toArray(bundles, String.class);
                    }
                    if (request.containsKey("approves")) {
                        final List<String> bundles = (List<String>) request.get("approves");
                        approvesArray = Iterables.toArray(bundles, String.class);
                    }
                    if (request.containsKey("bundles")) {
                        final List<String> bundles = (List<String>) request.get("bundles");
                        bundlesArray = Iterables.toArray(bundles, String.class);
                    }

                    FindTransactionResponse res = iotaAPI.findTransactions(
                            addressesArray,
                            tagsArray,
                            approvesArray,
                            bundlesArray);


                    final List<String> trytes = findTransactionStatementHash(findTransactionStatement(request));
                    trytes.addAll(Arrays.asList(res.getHashes())); // TODO optimize

                    return FindTransactionsResponse.create(trytes);

                }

                case "getTips": {
                    threads.iota.dto.response.GetTipsResponse res = iotaAPI.getTips();
                    checkNotNull(res);
                    return GetTipsResponse.create(Arrays.asList(res.getHashes()));
                }
                case "getTransactionsToApprove": {
                    final Optional<String> reference = request.containsKey("reference") ?
                            Optional.of(getParameterAsString(request, "reference"))
                            : Optional.empty();
                    final int depth = getParameterAsInt(request, "depth");


                    try {

                        threads.iota.dto.response.GetTransactionsToApproveResponse res = iotaAPI.getTransactionsToApprove(depth, reference.get()); // TODO add reference
                        checkNotNull(res);
                        // TODO optimize remove the hash stuff
                        return GetTransactionsToApproveResponse.create(
                                Hash.convertToHash(res.getBranchTransaction()),
                                Hash.convertToHash(res.getTrunkTransaction()));

                    } catch (RuntimeException e) {
                        Log.e(TAG, "Tip selection failed: " + e.getLocalizedMessage());
                        return ErrorResponse.create(e.getLocalizedMessage());
                    }
                }
                case "getTrytes": {
                    final List<String> hashes = getParameterAsList(request, "hashes", HASH_SIZE);

                    threads.iota.dto.response.GetTrytesResponse res = iotaAPI.getTrytes(Iterables.toArray(hashes, String.class));

                    List<String> trytes = getTrytesStatement(hashes);
                    trytes.addAll(Arrays.asList(res.getTrytes())); // TODO optimize
                    return GetTrytesResponse.create(trytes);
                }
                case "storeTransactions": {
                    try {
                        final List<String> trytes = getParameterAsList(request, "trytes", TRYTES_SIZE);
                        List<String> transfer = storeTransactions(trytes);
                        iotaAPI.storeTransactions(Iterables.toArray(transfer, String.class));
                        return AbstractResponse.createEmptyResponse();
                    } catch (RuntimeException e) {
                        //transaction not valid
                        return ErrorResponse.create("Invalid trytes input");
                    }
                }
                default: {
                    return ErrorResponse.create("Command [" + command + "] is unknown");
                }
            }

        } catch (final RuntimeException e) {
            Log.e(TAG, "Node Validation failed: " + e.getLocalizedMessage(), e);
            return ErrorResponse.create(e.getLocalizedMessage());
        } catch (final Throwable e) {
            Log.e(TAG, "Node Exception: ", e);
            return ExceptionResponse.create(e.getLocalizedMessage());
        }
    }


    private List<String> storeTransactions(final List<String> trytes) {
        List<String> transfer = new ArrayList<>();

        byte[] txTrits = Converter.allocateTritsForTrytes(TRYTES_SIZE);
        for (final String tryte : trytes) {

            //validate all trytes
            Converter.trits(tryte, txTrits, 0);
            // ReWi accept minWeightMagnitude with 1
            ITransactionStorage transactionStorage = TransactionValidator.validateTrits(
                    transactionDatabase, txTrits,
                    /*instance.transactionValidator.getMinWeightMagnitude()*/1);

            if (transactionStorage.getWeightMagnitude() >= IotaClient.MIN_WEIGHT_MAGNITUDE) {
                transfer.add(tryte);
            } else {
                // only with small minWeightMagnitude will be store locally (TODO maybe change in future)
                transactionDatabase.insertTransactionStorage(transactionStorage);
            }
        }

        return transfer;
    }

    private HashSet<String> getParameterAsSet(Map<String, Object> request, String paramName, int size) {

        HashSet<String> result = new HashSet<>(getParameterAsList(request, paramName, size));
        if (result.contains(Hash.NULL_HASH.toString())) {
            throw new RuntimeException("Invalid " + paramName + " input");
        }
        return result;
    }

    private synchronized List<String> findTransactionStatementHash(final Set<ITransactionStorage> transactionStorages) {
        final List<String> foundTransactions = new ArrayList<>();

        for (ITransactionStorage transactionStorage : transactionStorages) {
            foundTransactions.add(transactionStorage.getHash());
        }
        return foundTransactions;
    }


    private synchronized Set<ITransactionStorage> findTransactionStatement(final Map<String, Object> request) {
        final Set<ITransactionStorage> foundTransactions = new HashSet<>();
        boolean containsKey = false;

        final Set<ITransactionStorage> bundlesTransactions = new HashSet<>();
        if (request.containsKey("bundles")) {
            final HashSet<String> bundles = getParameterAsSet(request, "bundles", HASH_SIZE);
            for (final String bundle : bundles) {
                ITransactionStorage[] transactions = transactionDatabase.getBundles(bundle);
                Collections.addAll(bundlesTransactions, transactions);

            }
            foundTransactions.addAll(bundlesTransactions);
            containsKey = true;
        }

        final Set<ITransactionStorage> addressesTransactions = new HashSet<>();
        if (request.containsKey("addresses")) {

            final HashSet<String> addresses = getParameterAsSet(request, "addresses", HASH_SIZE);
            for (final String address : addresses) {
                ITransactionStorage[] transactions = transactionDatabase.getAddresses(address);
                Collections.addAll(addressesTransactions, transactions);
            }
            foundTransactions.addAll(addressesTransactions);
            containsKey = true;
        }

        final Set<ITransactionStorage> tagsTransactions = new HashSet<>();
        if (request.containsKey("tags")) {
            final HashSet<String> tags = getParameterAsSet(request, "tags", 0);
            for (String tag : tags) {
                ITransactionStorage[] transactions = transactionDatabase.getTags(tag);
                Collections.addAll(tagsTransactions, transactions);
            }
            if (tagsTransactions.isEmpty()) {
                for (String tag : tags) {
                    ITransactionStorage[] transactions = transactionDatabase.getObsoleteTags(tag);
                    Collections.addAll(tagsTransactions, transactions);
                }
            }
            foundTransactions.addAll(tagsTransactions);
            containsKey = true;
        }

        final Set<ITransactionStorage> approveeTransactions = new HashSet<>();

        if (request.containsKey("approvees")) {
            final HashSet<String> approvees = getParameterAsSet(request, "approvees", HASH_SIZE);
            for (final String approvee : approvees) {
                // TODO probably wrong
                ITransactionStorage transaction = transactionDatabase.getTransactionStorage(approvee);
                approveeTransactions.add(transaction);
            }
            foundTransactions.addAll(approveeTransactions);
            containsKey = true;
        }

        if (!containsKey) {
            throw new RuntimeException("Invalid parameters for findTransactionStatement");
        }

        //Using multiple of these input fields returns the intersection of the values.
        if (request.containsKey("bundles")) {
            foundTransactions.retainAll(bundlesTransactions);
        }
        if (request.containsKey("addresses")) {
            foundTransactions.retainAll(addressesTransactions);
        }
        if (request.containsKey("tags")) {
            foundTransactions.retainAll(tagsTransactions);
        }
        if (request.containsKey("approvees")) {
            foundTransactions.retainAll(approveeTransactions);
        }

        return foundTransactions;
    }

    private synchronized List<String> getTrytesStatement(List<String> hashes) {
        final List<String> elements = new LinkedList<>();
        for (final String hash : hashes) {
            ITransactionStorage transactionStorage = transactionDatabase.getTransactionStorage(hash);
            if (transactionStorage != null) {
                String tryte = transactionStorage.toTrytes();
                elements.add(tryte);
            }
        }
        return elements;
    }


    private NanoServer.Response sendResponse(NanoServer.IHTTPSession exchange, AbstractResponse res, long beginningTime) {
        res.setDuration((int) (System.currentTimeMillis() - beginningTime));


        setupResponseHeaders(exchange);

        NanoServer.Response.Status status = NanoServer.Response.Status.OK;
        if (res instanceof ErrorResponse) {
            status = NanoServer.Response.Status.BAD_REQUEST;
        } else if (res instanceof AccessLimitedResponse) {
            status = NanoServer.Response.Status.UNAUTHORIZED;
        } else if (res instanceof ExceptionResponse) {
            status = NanoServer.Response.Status.INTERNAL_ERROR;
        }
        final String response = gson.toJson(res);
        byte[] body = response.getBytes();
        return threads.iota.daemon.WebServer.newFixedLengthResponse(status, NanoServer.MIME_PLAINTEXT, new ByteArrayInputStream(body), body.length);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
