package threads.share;

import android.os.Parcel;
import android.os.Parcelable;

public class PDFConfig implements Parcelable {

    public static final String EXTRA_CONFIG = "PDFConfig";
    public static final Creator<PDFConfig> CREATOR = new Creator<PDFConfig>() {
        @Override
        public PDFConfig createFromParcel(Parcel in) {
            return new PDFConfig(in);
        }

        @Override
        public PDFConfig[] newArray(int size) {
            return new PDFConfig[size];
        }
    };
    private String cid;
    private int swipeorientation;

    public PDFConfig() {

    }


    protected PDFConfig(Parcel in) {
        this.cid = in.readString();
        this.swipeorientation = in.readInt();
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public int getSwipeorientation() {
        return swipeorientation;
    }

    public void setSwipeorientation(int swipeorientation) {
        this.swipeorientation = swipeorientation;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cid);
        dest.writeInt(swipeorientation);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
