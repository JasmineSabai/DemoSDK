package com.jasmine.demosdk

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.jasmine.demosdk.databinding.ActivityMainBinding
import com.jasmine.demosdk.emv.PcscProvider
import com.jasmine.demosdk.emv.Utils
import com.jasmine.demosdk.emv.parser.NdefMessageParser
import com.jasmine.demosdk.emv.record.ParsedNdefRecord
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId


class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private var mNfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

//        binding.text = "NFC"

        if (mNfcAdapter == null) {
            binding.text = "No NFC"
//            Toast.makeText(this, "No NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, this.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), FLAG_MUTABLE
        )
//        var response = ""
//        response = CustomApiRequest().makeGet("TESTING_PARAM_1")
//        response += "\n"+CustomApiRequest().makePost("TESTING_PARAM_2")

        binding.text = "NFC"

    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        resolveIntent(intent!!)
        Toast.makeText(this, "AAA: "+ intent!!.action, Toast.LENGTH_LONG).show()

    }

    private fun showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

//        if (mNfcAdapter != null) {
//            if (!mNfcAdapter!!.isEnabled())
//                showWirelessSettings();
//
//            mNfcAdapter!!.enableForegroundDispatch(this, pendingIntent, null, null);
//        }

        if (mNfcAdapter != null) {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

            mNfcAdapter!!.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options
            )
        }

    }

    private fun resolveIntent(intent: Intent) {
        val action = intent.action

        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, Tag::class.java)
            } else {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }

//            val ndefMessage: NdefMessage = this.getNdefMessageFromIntent(getIntent())
//            val msgs: Array<NdefMessage?>
//            if (rawMsgs != null) {
//                msgs = arrayOfNulls(rawMsgs.size)
//                for (i in rawMsgs.indices) {
//                    msgs[i] = rawMsgs[i] as NdefMessage
//                }
//            } else {
//                val empty = ByteArray(0)
//                val id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)
//                val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag?
//                val payload: ByteArray = dumpTagData(tag!!).toByteArray()
//                val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload)
//                val msg = NdefMessage(arrayOf(record))
//                msgs = arrayOf(msg)
//            }
//            displayMsgs(msgs)
        }
    }

    private fun displayMsgs(msgs: Array<NdefMessage?>) {
        if (msgs.isEmpty()) return
        val builder = StringBuilder()
        val records: List<ParsedNdefRecord> = NdefMessageParser.parse(msgs[0]!!)
        val size = records.size
        for (i in 0 until size) {
            val record: ParsedNdefRecord = records[i]
            val str: String? = record.str()
            builder.append(str).append("\n")
        }
        binding.text = builder.toString()
    }

    private fun dumpTagData(tag: Tag): String {
        val sb = java.lang.StringBuilder()
        val id = tag.id
        sb.append("ID (hex): ").append(Utils.toHex(id)).append('\n')
        sb.append("ID (reversed hex): ").append(Utils.toReversedHex(id)).append('\n')
        sb.append("ID (dec): ").append(Utils.toDec(id)).append('\n')
        sb.append("ID (reversed dec): ").append(Utils.toReversedDec(id)).append('\n')
        val prefix = "android.nfc.tech."
        sb.append("Technologies: ")
        for (tech in tag.techList) {
            sb.append(tech.substring(prefix.length))
            sb.append(", ")
        }
        sb.delete(sb.length - 2, sb.length)
        for (tech in tag.techList) {
            if (tech == MifareClassic::class.java.name) {
                sb.append('\n')
                var type = "Unknown"
                try {
                    val mifareTag = MifareClassic.get(tag)
                    when (mifareTag.type) {
                        MifareClassic.TYPE_CLASSIC -> type = "Classic"
                        MifareClassic.TYPE_PLUS -> type = "Plus"
                        MifareClassic.TYPE_PRO -> type = "Pro"
                    }
                    sb.append("Mifare Classic type: ")
                    sb.append(type)
                    sb.append('\n')
                    sb.append("Mifare size: ")
                    sb.append(mifareTag.size.toString() + " bytes")
                    sb.append('\n')
                    sb.append("Mifare sectors: ")
                    sb.append(mifareTag.sectorCount)
                    sb.append('\n')
                    sb.append("Mifare blocks: ")
                    sb.append(mifareTag.blockCount)
                } catch (e: Exception) {
                    sb.append("Mifare classic error: " + e.message)
                }
            }
            if (tech == MifareUltralight::class.java.name) {
                sb.append('\n')
                val mifareUlTag = MifareUltralight.get(tag)
                var type = "Unknown"
                when (mifareUlTag.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> type = "Ultralight"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> type = "Ultralight C"
                }
                sb.append("Mifare Ultralight type: ")
                sb.append(type)
            }
        }
        return sb.toString()
    }

    override fun onTagDiscovered(tag: Tag?) {
            val isoDep: IsoDep?
            try {
                isoDep = IsoDep.get(tag)
                if (isoDep != null) {
                    (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                        VibrationEffect.createOneShot(
                            150,
                            10
                        )
                    )
                }
                isoDep.connect()
                val provider = PcscProvider()
                provider.setmTagCom(isoDep)
//                val provider = PcscProvider()
                // Create provider
// Define config
                val config: EmvTemplate.Config = EmvTemplate.Config()
                    .setContactLess(true) // Enable contact less reading (default: true)
                    .setReadAllAids(true) // Read all aids in card (default: true)
                    .setReadTransactions(true) // Read all transactions (default: true)
                    .setReadCplc(false) // Read and extract CPCLC data (default: false)
                    .setRemoveDefaultParsers(false) // Remove default parsers for GeldKarte and EmvCard (default: false)
                    .setReadAt(true) // Read and extract ATR/ATS and description

// Create Parser
// Create Parser
                val parser = EmvTemplate.Builder() //
                    .setProvider(provider) // Define provider
                    .setConfig(config) // Define config
                    //.setTerminal(terminal) (optional) you can define a custom terminal implementation to create APDU
                    .build()
                val card = parser.readEmvCard()
                val cardNumber = card.cardNumber
                binding.text = "Card Number: $cardNumber"

                val expireDate = card.expireDate
                var date = LocalDate.of(1999, 12, 31)
                if (expireDate != null) {
                    date = expireDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                binding.text += "\nCard Expiry Date: $date"
                binding.text += "\nCard Type: ${card.type}"
                binding.text += "\nCardholder Name: ${card.holderFirstname} ${card.holderLastname}"
//                binding.text += "\nCard Track1: ${card.track1}"
//                binding.text += "\nCard Track2: ${card.track2}"
//                binding.text = card.toString()
                try {
                    isoDep.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

}