#pragma version(1)
#pragma rs_fp_relaxed
#pragma rs java_package_name(threads.server)

#include "rs_debug.rsh"
#include "rs_core.rsh"

#define HIGH_BITS 0xFFFFFFFFFFFFFFFF
#define LOW_BITS 0x0000000000000000
#define LOW_0 0xDB6DB6DB6DB6DB6D
#define HIGH_0 0xB6DB6DB6DB6DB6DB
#define LOW_1 0xF1F8FC7E3F1F8FC7
#define HIGH_1 0x8FC7E3F1F8FC7E3F
#define LOW_2 0x7FFFE00FFFFC01FF
#define HIGH_2 0xFFC01FFFF803FFFF
#define LOW_3 0xFFC0000007FFFFFF
#define HIGH_3 0x003FFFFFFFFFFFFF

volatile int found; // done
int minWeightMagnitude; // done
int8_t* trits;  // done

typedef struct {
    long midStateLow[729];
    long midStateHigh[729];
    long stateLow[729];
    long stateHigh[729];
    long nonce;
} State;



#pragma rs reduce(doSearch) initializer(initializerSearch) accumulator(accumulatorSearch) combiner(combinerSearch) outconverter(outconverterSearch)

static void initializerSearch(State *accum) {

    int i1, i2, i3;
    long alpha, beta, gamma, delta;
    long scratchpadLow[729];
    long scratchpadHigh[729];
    int offset = 0;

    rsDebug("Init", found);

    accum->nonce = 0;

    if(found > 0 ) return; // abort

    for (i1 = 243; i1 < 729; i1++) {
        accum->midStateLow[i1] = HIGH_BITS;
        accum->midStateHigh[i1] = HIGH_BITS;
    }


    for (i1 = 31; i1 >= 0; i1--) {
        for (i2 = 0; i2 < 243; i2++) {
            int8_t value = trits[offset];

            offset = offset + 1;
            if (value == 0) {
                accum->midStateLow[i2] = HIGH_BITS;
                accum->midStateHigh[i2] = HIGH_BITS;

            } else if (value == 1) {
                accum->midStateLow[i2] = LOW_BITS;
                accum->midStateHigh[i2] = HIGH_BITS;
            } else {
                accum->midStateLow[i2] = HIGH_BITS;
                accum->midStateHigh[i2] = LOW_BITS;
            }
        }


        int scratchpadIndex = 0;

        for (i2 = 81; i2-- > 0;) {

            for (i3 = 0; i3 < 729; i3++) {
                scratchpadLow[i3] = accum->midStateLow[i3];
                scratchpadHigh[i3] = accum->midStateHigh[i3];
            }

            for (i3 = 0; i3 < 729; i3++) {

                alpha = scratchpadLow[scratchpadIndex];
                beta = scratchpadHigh[scratchpadIndex];
                gamma = scratchpadHigh[scratchpadIndex +=
                                                      (scratchpadIndex < 365 ? 364 : -365)];
                delta = (alpha | (~gamma)) & (scratchpadLow[scratchpadIndex] ^ beta);

                accum->midStateLow[i3] = ~delta;
                accum->midStateHigh[i3] = (alpha ^ gamma) | delta;
            }
        }

    }


    for (i1 = 0; i1 < 162; i1++) {

        int8_t value = trits[offset];
        offset = offset + 1;
        if (value == 0) {
            accum->midStateLow[i1] = HIGH_BITS;
            accum->midStateHigh[i1] = HIGH_BITS;

        } else if (value == 1) {
            accum->midStateLow[i1] = LOW_BITS;
            accum->midStateHigh[i1] = HIGH_BITS;
        } else {
            accum->midStateLow[i1] = HIGH_BITS;
            accum->midStateHigh[i1] = LOW_BITS;
        }
    }


    accum->midStateLow[162 + 0] = LOW_0;
    accum->midStateHigh[162 + 0] = HIGH_0;
    accum->midStateLow[162 + 1] = LOW_1;
    accum->midStateHigh[162 + 1] = HIGH_1;
    accum->midStateLow[162 + 2] = LOW_2;
    accum->midStateHigh[162 + 2] = HIGH_2;
    accum->midStateLow[162 + 3] = LOW_3;
    accum->midStateHigh[162 + 3] = HIGH_3;

}

static void accumulatorSearch(State *accum, int current,
                                            int x /* special arg */,
                                            int y /* special arg */) {

    int i1, i2, i3;
    long alpha, beta, gamma, delta, low, hi;
    long scratchpadLow[729];
    long scratchpadHigh[729];
    rsDebug("Accu", current);
    if(found > 0 ) return; // abort

    for (i1 = current; i1-- > 0;) {
        long carry = 1;
        for (i2 = 189; i2 < 216 && carry != 0; i2++) {
            low = accum->midStateLow[i2];
            hi = accum->midStateHigh[i2];
            accum->midStateLow[i2] = hi ^ low;
            accum->midStateHigh[i2] = low;
            carry = hi & (~low);
        }
    }


    int maskStartIndex = 243 - minWeightMagnitude;
    long mask = 0;

    while (found == 0) {
        long carry = 1;

        //rsDebug("Check Found", found);

        for (i2 = 216; i2 < 243 && carry != 0; i2++) {
            low = accum->midStateLow[i2];
            hi = accum->midStateHigh[i2];
            accum->midStateLow[i2] = hi ^ low;
            accum->midStateHigh[i2] = low;
            carry = hi & (~low);
        }


        for (i2 = 0; i2 < 729; i2++) {
            accum->stateLow[i2] = accum->midStateLow[i2];
            accum->stateHigh[i2] = accum->midStateHigh[i2];
        }


        int scratchpadIndex = 0;

        for (i2 = 81; i2-- > 0;) {


            for (i3 = 0; i3 < 729; i3++) {
                scratchpadLow[i3] = accum->stateLow[i3];
                scratchpadHigh[i3] = accum->stateHigh[i3];
            }

            for (i3 = 0; i3 < 729; i3++) {

                alpha = scratchpadLow[scratchpadIndex];
                beta = scratchpadHigh[scratchpadIndex];
                gamma = scratchpadHigh[scratchpadIndex +=
                                                      (scratchpadIndex < 365 ? 364 : -365)];
                delta = (alpha | (~gamma)) & (scratchpadLow[scratchpadIndex] ^ beta);

                accum->stateLow[i3] = ~delta;
                accum->stateHigh[i3] = (alpha ^ gamma) | delta;
            }
        }


        mask = HIGH_BITS;
        for (i1 = maskStartIndex; i1 < 243 && mask != 0; i1++) {
            mask &= ~(accum->stateLow[i1] ^ accum->stateHigh[i1]);
        }

        if (mask != 0 && found == 0) {
            rsAtomicInc(&found);
            rsDebug("Found", found);

            long outMask = 1;
            while ((outMask & mask) == 0) {
                outMask <<= 1;
            }
            accum->nonce = outMask;


            for (int i1 = 0; i1 < 243; i1++) {
                int8_t value = (accum->midStateLow[i1] & accum->nonce) == 0 ? 1:(accum->midStateHigh[i1] & accum->nonce) == 0?  -1:  0;
                trits[7776+i1] = value;
            }

            return;

        }
    }
    accum->nonce = 0;
}

static void combinerSearch(State *accum, const State *other) {

    rsDebug("Comb", 0);
    if (other->nonce > accum->nonce) {
        accum->nonce = other->nonce;
        for (int i = 0; i < 729; i++) {
            accum->midStateLow[i] = other->midStateLow[i];
            accum->midStateHigh[i] = other->midStateHigh[i];
        }
    }
}

static void outconverterSearch(long *result, const State *accum) {

    rsDebug("Out", accum->nonce);
    *result = accum->nonce;


}
