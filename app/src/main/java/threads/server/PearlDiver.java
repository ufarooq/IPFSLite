package threads.server;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import com.iota.iri.utils.Converter;

import threads.iri.client.IPoW;
import threads.iri.tangle.TangleUtils;

public class PearlDiver implements IPoW {
    private final String TAG = PearlDiver.class.getSimpleName();
    private final RenderScript rs;

    public PearlDiver(@NonNull Context context) {
        rs = RenderScript.create(context);
    }


    @Override
    public synchronized String performPoW(String trytes, int minWeightMagnitude) {


        byte[] trits = Converter.allocateTritsForTrytes(TangleUtils.TRYTES_SIZE);
        Converter.trits(trytes, trits, 0);

        // Load script
        ScriptC_search mScript = new ScriptC_search(rs);
        mScript.set_found(0);
        mScript.set_minWeightMagnitude(minWeightMagnitude);


        Allocation tritsAllo = Allocation.createSized(rs, Element.I8(rs), 8019);
        tritsAllo.copy1DRangeFrom(0, 8019, trits);
        mScript.bind_trits(tritsAllo);

        int[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};


        Allocation ain1 = Allocation.createSized(rs, Element.I32(rs), a.length);
        ain1.setAutoPadding(true);
        ain1.copyFrom(a);

        ScriptC_search.result_long res = mScript.reduce_doSearch(a);

        Log.e(TAG, "" + res.get());

        tritsAllo.copy1DRangeTo(0, 8019, trits);


        return Converter.trytes(trits);

    }

}
