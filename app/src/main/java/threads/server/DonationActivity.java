package threads.server;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DonationActivity extends AppCompatActivity {
    private static final String TAG = "DonationActivity";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        String donationAddress = Application.getDonationsAddress();
        try {
            Bitmap bitmap = net.glxn.qrgen.android.QRCode.from(donationAddress).
                    withSize(Application.QR_CODE_SIZE, Application.QR_CODE_SIZE).bitmap();
            ImageView donations_address_qr_code = findViewById(R.id.donations_address_qr_code);
            donations_address_qr_code.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        TextInputEditText donations_address = findViewById(R.id.donations_address);
        donations_address.setText(donationAddress);
        donations_address.selectAll();
        donations_address.setKeyListener(null);

        TextView donations_to_clipboard = findViewById(R.id.donations_to_clipboard);
        donations_to_clipboard.setOnClickListener((v) -> {

            donations_address.requestFocus();
            donations_address.selectAll();

            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(
                    getString(R.string.copy_address_to_clipboard),
                    donationAddress);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(getApplicationContext(),
                    "Copied " + donationAddress + " to clipboard",
                    Toast.LENGTH_LONG).show();


        });

        TextView donations_tangle_details = findViewById(R.id.donations_tangle_details);
        String addressLink = Application.getHtmlAddressLink(
                this.getString(R.string.details),
                donationAddress, false);
        donations_tangle_details.setText(
                Html.fromHtml(addressLink, Html.FROM_HTML_MODE_LEGACY));
        donations_tangle_details.setClickable(true);
        donations_tangle_details.setOnClickListener((v) -> {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(Application.getAddressLink(donationAddress, false)));
            DonationActivity.this.startActivity(intent);

        });
    }


}
