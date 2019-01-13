package com.example.fletch.nfcwriterandreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    enum mode {READ, WRITE, NONE}

    private Button mReadButton;
    private Button mWriteButton;
    private TextView mStatus;
    private NfcAdapter mNfcAdapter;
    private mode mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mReadButton = findViewById(R.id.button_read);
        mWriteButton = findViewById(R.id.button_write);
        mStatus = findViewById(R.id.textView_status);


        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        boolean isAvailable = mNfcAdapter != null && mNfcAdapter.isEnabled();
        if (isAvailable) {
            mStatus.setText("Technologie NFC disponible");
        } else {
            mStatus.setText("Technologie NFC non-disponible");
        }

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReadMode();
            }
        });

        mWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWriteMode();
            }
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            switch (mMode) {
                case READ:
                    read(intent);
                    break;
                case WRITE:
                    write(intent);
                    break;
            }

        }

        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter == null)
            return;

        // Dispatch les intents à l'activity
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);
    }



    private void setWriteMode() {
        mStatus.setText("Write mode");
        this.mMode = mode.WRITE;

    }

    private void setReadMode() {
        mStatus.setText("Read mode");
        this.mMode = mode.READ;
    }

    /**
     * Lecture du texte et affichage dans la barre de status
     * @param intent
     */
    private void read(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        byte[] payload = msg.getRecords()[0].getPayload();
        String languageCode = "";
        try {
            languageCode = new String(payload, 1
                    , payload[0] & Byte.parseByte("00111111", 2), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            mStatus.setText(e.getMessage());
        }
        ((TextView) findViewById(R.id.textView_read)).setText(new String(payload, languageCode.length() + 1
                , payload.length - languageCode.length() - 1));
    }

    /**
     * Écriture du texte
     * @param intent
     * @return True si écriture réussie
     */
    private boolean write(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord ndefRecord = NdefRecord.createTextRecord(null
                ,((TextView)findViewById(R.id.editText_write)).getText().toString());
        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        try {

            if (tag != null) {

                Ndef ndef = Ndef.get(tag);
                if (ndef == null) {
                    try {
                        NdefFormatable ndefFormat = NdefFormatable.get(tag);

                        if (ndefFormat != null) {
                            ndefFormat.connect();
                            ndefFormat.format(ndefMessage);
                            ndefFormat.close();
                            return true;
                        }

                    } catch (Exception e) {
                        mStatus.setText(e.getMessage());
                    }

                } else {
                    ndef.connect();

                    if (ndef.isWritable()) {
                        ndef.writeNdefMessage(ndefMessage);

                        ndef.close();
                        return true;
                    }

                    ndef.close();
                }
            }

        } catch (Exception e) {
           mStatus.setText(e.getMessage());
        }
        return false;
    }
}

