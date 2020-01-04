package threads.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.server.R;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class PDFViewActivity extends AppCompatActivity implements IShowPage {

    private static final String TAG = PDFViewActivity.class.getSimpleName();
    private static final String STATE_CURRENT_PAGE_INDEX = "current_page_index";
    private PDFViewPager pdfviewpager;
    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private int mPageIndex;
    private PDFConfig config;
    private File myTempFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        config = intent.getParcelableExtra(PDFConfig.EXTRA_CONFIG);

        setContentView(R.layout.activity_pdf);
        pdfviewpager = findViewById(R.id.pdf_view_pager);
        pdfviewpager.setSwipeOrientation(config.getSwipeorientation());

        mPageIndex = 0;
        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        if (null != savedInstanceState) {
            mPageIndex = savedInstanceState.getInt(STATE_CURRENT_PAGE_INDEX, 0);
        }


        //render the pdf view
        try {
            openRenderer(this);
            setUpViewPager();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void setUpViewPager() {
        PDFAdapter adapter = new PDFAdapter(
                PDFViewActivity.this, this, mPdfRenderer.getPageCount());
        pdfviewpager.setAdapter(adapter);
        pdfviewpager.setCurrentItem(mPageIndex);
    }

    @Override
    protected void onDestroy() {
        try {
            closeRenderer();
        } catch (IOException e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (null != mCurrentPage) {
            outState.putInt(STATE_CURRENT_PAGE_INDEX, mCurrentPage.getIndex());
        }
    }


    private void openRenderer(Context context) throws IOException {

        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs);
        int timeout = Preferences.getConnectionTimeout(context);
        myTempFile = ipfs.getTempCacheFile();
        ipfs.storeToFile(myTempFile, CID.create(config.getCid()),
                true, timeout, -1);

        mFileDescriptor = ParcelFileDescriptor.open(myTempFile, ParcelFileDescriptor.MODE_READ_ONLY);
        if (mFileDescriptor != null) {
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        }

    }


    private void closeRenderer() throws IOException {

        if (null != mCurrentPage)
            mCurrentPage.close();

        if (null != mPdfRenderer)
            mPdfRenderer.close();

        if (null != mFileDescriptor)
            mFileDescriptor.close();

        if (null != myTempFile) {
            if (myTempFile.exists()) {
                checkArgument(myTempFile.delete());
            }
        }
    }

    public Bitmap showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return null;
        }
        // Make sure to close the current page before opening another one.
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        return bitmap;
    }

}
