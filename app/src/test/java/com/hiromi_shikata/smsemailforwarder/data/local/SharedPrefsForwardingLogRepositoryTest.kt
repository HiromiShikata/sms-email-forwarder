package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.SharedPreferences
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SharedPrefsForwardingLogRepositoryTest {

    private val editor: SharedPreferences.Editor = mock<SharedPreferences.Editor>().also {
        whenever(it.putString(any(), any())).thenReturn(it)
    }
    private val prefs: SharedPreferences = mock<SharedPreferences>().also {
        whenever(it.edit()).thenReturn(editor)
        whenever(it.getString(any(), any())).thenReturn(null)
    }
    private val repository = SharedPrefsForwardingLogRepository(prefs)

    @Test
    fun `getAll returns empty list when no entries stored`() {
        whenever(prefs.getString(any(), any())).thenReturn(null)

        val result = repository.getAll()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `save stores entry and getAll returns it via serialization round trip`() {
        val entry = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null)

        val capturedJson = mutableListOf<String>()
        whenever(editor.putString(any(), any())).thenAnswer { invocation ->
            capturedJson.add(invocation.getArgument(1))
            editor
        }

        repository.save(entry)

        assertTrue(capturedJson.isNotEmpty())
        val deserializedEntries = deserializeEntries(capturedJson.last())
        assertEquals(1, deserializedEntries.size)
        assertEquals(entry, deserializedEntries[0])
    }

    @Test
    fun `save accumulates multiple entries`() {
        val entries = mutableListOf<String>()
        whenever(editor.putString(any(), any())).thenAnswer { invocation ->
            entries.add(invocation.getArgument(1))
            editor
        }

        val entry1 = ForwardingLogEntry(1000L, "+1111111111", ForwardingLogEntryStatus.FORWARDED, null)
        val entry2 = ForwardingLogEntry(2000L, "+2222222222", ForwardingLogEntryStatus.FAILED, "error")

        repository.save(entry1)

        whenever(prefs.getString(any(), any())).thenReturn(entries.last())

        repository.save(entry2)

        val result = deserializeEntries(entries.last())
        assertEquals(2, result.size)
        assertEquals(entry1, result[0])
        assertEquals(entry2, result[1])
    }

    @Test
    fun `save stores FAILED entry with error message`() {
        val entry = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FAILED, "Connection refused")

        val capturedJson = mutableListOf<String>()
        whenever(editor.putString(any(), any())).thenAnswer { invocation ->
            capturedJson.add(invocation.getArgument(1))
            editor
        }

        repository.save(entry)

        val result = deserializeEntries(capturedJson.last())
        assertEquals("Connection refused", result[0].errorMessage)
    }

    @Test
    fun `save stores SETUP_INCOMPLETE entry with null error message`() {
        val entry = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.SETUP_INCOMPLETE, null)

        val capturedJson = mutableListOf<String>()
        whenever(editor.putString(any(), any())).thenAnswer { invocation ->
            capturedJson.add(invocation.getArgument(1))
            editor
        }

        repository.save(entry)

        val result = deserializeEntries(capturedJson.last())
        assertNull(result[0].errorMessage)
    }
}

class SerializationTest {
    @Test
    fun `serializeEntry and deserializeEntry round trip preserves all fields`() {
        val entry = ForwardingLogEntry(1234567890L, "+9876543210", ForwardingLogEntryStatus.FAILED, "SMTP error msg")

        val json = serializeEntry(entry)
        val result = deserializeEntry(json)

        assertEquals(entry, result)
    }

    @Test
    fun `deserializeEntry returns null for FORWARDED entry without error message`() {
        val entry = ForwardingLogEntry(1000L, "+1111", ForwardingLogEntryStatus.FORWARDED, null)

        val json = serializeEntry(entry)
        val result = deserializeEntry(json)

        assertNull(result!!.errorMessage)
    }

    @Test
    fun `serializeEntries and deserializeEntries round trip preserves order`() {
        val entries = listOf(
            ForwardingLogEntry(1000L, "+1111", ForwardingLogEntryStatus.FORWARDED, null),
            ForwardingLogEntry(2000L, "+2222", ForwardingLogEntryStatus.FAILED, "err"),
            ForwardingLogEntry(3000L, "+3333", ForwardingLogEntryStatus.SETUP_INCOMPLETE, null),
        )

        val json = serializeEntries(entries)
        val result = deserializeEntries(json)

        assertEquals(entries, result)
    }
}

class EnforceMaxEntriesTest {
    @Test
    fun `enforceMaxEntries keeps all entries when under limit`() {
        val entries = (1..50).map {
            ForwardingLogEntry(it.toLong(), "+$it", ForwardingLogEntryStatus.FORWARDED, null)
        }

        val result = enforceMaxEntries(entries)

        assertEquals(50, result.size)
    }

    @Test
    fun `enforceMaxEntries drops oldest when over limit`() {
        val entries = (1..51).map {
            ForwardingLogEntry(it.toLong(), "+$it", ForwardingLogEntryStatus.FORWARDED, null)
        }

        val result = enforceMaxEntries(entries)

        assertEquals(50, result.size)
        assertEquals(2L, result.first().timestamp)
        assertEquals(51L, result.last().timestamp)
    }

    @Test
    fun `enforceMaxEntries keeps exactly max size entries`() {
        val entries = (1..60).map {
            ForwardingLogEntry(it.toLong(), "+$it", ForwardingLogEntryStatus.FORWARDED, null)
        }

        val result = enforceMaxEntries(entries)

        assertEquals(50, result.size)
        assertEquals(11L, result.first().timestamp)
        assertEquals(60L, result.last().timestamp)
    }
}
