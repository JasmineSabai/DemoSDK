package com.jasmine.demosdk.emv.parser

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.jasmine.demosdk.emv.record.ParsedNdefRecord
import com.jasmine.demosdk.emv.record.SmartPoster
import com.jasmine.demosdk.emv.record.TextRecord
import com.jasmine.demosdk.emv.record.UriRecord

object NdefMessageParser {
    fun parse(message: NdefMessage): List<ParsedNdefRecord> {
        return getRecords(message.records)
    }

    fun getRecords(records: Array<NdefRecord>): List<ParsedNdefRecord> {
        val elements: MutableList<ParsedNdefRecord> = ArrayList()
        for (record in records) {
            if (UriRecord.isUri(record)) {
                elements.add(UriRecord.parse(record))
            } else if (TextRecord.isText(record)) {
                elements.add(TextRecord.parse(record))
            } else if (SmartPoster.isPoster(record)) {
                elements.add(SmartPoster.parse(record))
            } else {
                elements.add(object : ParsedNdefRecord {
                    override fun str(): String {
                        return String(record.payload)
                    }
                })
            }
        }
        return elements
    }
}