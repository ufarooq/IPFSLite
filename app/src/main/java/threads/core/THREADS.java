package threads.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import threads.core.api.EventsDatabase;
import threads.core.api.LinkType;
import threads.core.api.Note;
import threads.core.api.NoteType;
import threads.core.api.PeersDatabase;
import threads.core.api.PeersInfoDatabase;
import threads.core.api.Thread;
import threads.core.api.ThreadsAPI;
import threads.core.api.ThreadsDatabase;
import threads.iota.EntityService;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class THREADS extends ThreadsAPI {
    public static final String TAG = THREADS.class.getSimpleName();


    private THREADS(final THREADS.Builder builder) {
        super(builder.threadsDatabase, builder.eventsDatabase, builder.peersInfoDatabase,
                builder.peersDatabase, builder.entityService);
    }


    @NonNull
    public static String getAddress(@NonNull CID cid) {
        checkNotNull(cid);
        String address = TrytesConverter.asciiToTrytes(cid.getCid());
        return IOTA.addChecksum(address.substring(0, Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));
    }


    @NonNull
    public static THREADS createThreads(@NonNull ThreadsDatabase threadsDatabase,
                                        @NonNull EventsDatabase eventsDatabase,
                                        @NonNull PeersInfoDatabase peersInfoDatabase,
                                        @NonNull PeersDatabase peersDatabase,
                                        @NonNull EntityService entityService) {
        checkNotNull(threadsDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);
        checkNotNull(entityService);
        return new THREADS.Builder()
                .threadsDatabase(threadsDatabase)
                .peersInfoDatabase(peersInfoDatabase)
                .peersDatabase(peersDatabase)
                .eventsDatabase(eventsDatabase)
                .entityService(entityService)
                .build();
    }

    @Nullable
    public static LinkType getLinkType(@NonNull Note note) {
        checkNotNull(note);

        if (note.getNoteType() == NoteType.LINK) {
            String linkType = note.getAdditionalValue(LinkType.class.getSimpleName());
            return LinkType.valueOf(linkType);
        }
        return null;
    }

    public static double getLatitude(@NonNull Note note) {
        checkNotNull(note);

        if (note.getNoteType() == NoteType.LOCATION) {
            return Double.valueOf(note.getAdditionalValue(Preferences.LATITUDE));
        }
        return Double.NaN;
    }

    public static double getLongitude(@NonNull Note note) {
        checkNotNull(note);

        if (note.getNoteType() == NoteType.LOCATION) {
            return Double.valueOf(note.getAdditionalValue(Preferences.LONGITUDE));
        }
        return Double.NaN;
    }

    public static double getZoom(@NonNull Note note) {
        checkNotNull(note);

        if (note.getNoteType() == NoteType.LOCATION) {
            return Double.valueOf(note.getAdditionalValue(Preferences.ZOOM));
        }
        return Double.NaN;
    }

    @NonNull
    public static Date getTomorrow() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @NonNull
    public static Date getYesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }


    @NonNull
    public static String getDate(@NonNull Date date) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date today = c.getTime();
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 0);
        Date lastYear = c.getTime();

        if (date.before(today)) {
            if (date.before(lastYear)) {
                return android.text.format.DateFormat.format("dd.MM.yyyy", date).toString();
            } else {
                return android.text.format.DateFormat.format("dd.MMMM", date).toString();
            }
        } else {
            return android.text.format.DateFormat.format("HH:mm", date).toString();
        }
    }

    @NonNull
    public static Calendar getUserAgeOffsetCalendar(int offset) {
        Date now = new Date();
        Calendar init = new GregorianCalendar();
        init.setTime(now);
        int mYear = init.get(Calendar.YEAR);
        int mMonth = init.get(Calendar.MONTH);
        int mDay = init.get(Calendar.DAY_OF_MONTH);

        return new GregorianCalendar(mYear - offset, mMonth, mDay);
    }

    static long getOffsetDate(int offsetInYears) {
        return getUserAgeOffsetCalendar(offsetInYears).getTime().getTime();
    }

    @NonNull
    public static Date getOffsetYearDate(int offsetInYears) {
        return getUserAgeOffsetCalendar(offsetInYears).getTime();
    }

    public static long getTodayDate() {
        return getToday().getTime();
    }

    @NonNull
    public static Date getToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }


    public void setImage(@NonNull IPFS ipfs,
                         @NonNull Thread thread,
                         @NonNull byte[] data) throws Exception {
        checkNotNull(ipfs);
        checkNotNull(thread);
        checkNotNull(data);
        CID image = ipfs.storeData(data);
        if (image != null) {
            setImage(thread, image);
        }
    }


    public static class Builder {
        EventsDatabase eventsDatabase = null;
        ThreadsDatabase threadsDatabase = null;
        PeersInfoDatabase peersInfoDatabase = null;
        EntityService entityService = null;
        PeersDatabase peersDatabase = null;

        public THREADS build() {
            checkNotNull(threadsDatabase);
            checkNotNull(eventsDatabase);
            checkNotNull(peersInfoDatabase);
            checkNotNull(peersDatabase);
            checkNotNull(entityService);
            return new THREADS(this);
        }

        public Builder threadsDatabase(@NonNull ThreadsDatabase threadsDatabase) {
            checkNotNull(threadsDatabase);
            this.threadsDatabase = threadsDatabase;
            return this;
        }

        public Builder eventsDatabase(@NonNull EventsDatabase eventsDatabase) {
            checkNotNull(eventsDatabase);
            this.eventsDatabase = eventsDatabase;
            return this;
        }

        public Builder peersInfoDatabase(@NonNull PeersInfoDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersInfoDatabase = peersDatabase;
            return this;
        }

        public Builder entityService(@NonNull EntityService entityService) {
            checkNotNull(entityService);
            this.entityService = entityService;
            return this;
        }

        public Builder peersDatabase(@NonNull PeersDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersDatabase = peersDatabase;
            return this;
        }
    }
}
