package com.brahm.crm.util

import android.content.Context
import android.provider.ContactsContract
import com.brahm.crm.data.AppDatabase
import com.brahm.crm.data.Lead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactsImporter {

    data class Result(val imported: Int, val skipped: Int)

    /** Reads device contacts and adds new ones as leads (stage = New). */
    suspend fun importInto(context: Context, db: AppDatabase): Result =
        withContext(Dispatchers.IO) {
            var imported = 0
            var skipped = 0
            val resolver = context.contentResolver
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = HashSet<String>()
                while (c.moveToNext()) {
                    val name = if (nameIdx >= 0) c.getString(nameIdx)?.trim().orEmpty() else ""
                    val rawNum = if (numIdx >= 0) c.getString(numIdx)?.trim().orEmpty() else ""
                    val phone = normalize(rawNum)
                    if (name.isEmpty() || phone.isEmpty()) continue
                    if (!seen.add(phone)) continue
                    if (db.leadDao().phoneExists(phone)) {
                        skipped++
                        continue
                    }
                    db.leadDao().upsert(
                        Lead(name = name, phone = phone, source = "Contacts")
                    )
                    imported++
                }
            }
            Result(imported, skipped)
        }

    private fun normalize(number: String): String {
        val cleaned = number.filter { it.isDigit() || it == '+' }
        return cleaned
    }
}
