package com.hiromi_shikata.smsemailforwarder.presentation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.databinding.ActivityForwardingLogBinding
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingLogGetUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatLogAsText(entries: List<ForwardingLogEntry>): String =
    entries.joinToString("\n") { entry ->
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(entry.timestamp))
        val errorPart = if (entry.status == ForwardingLogEntryStatus.FAILED && entry.errorMessage != null) {
            " [${entry.errorMessage}]"
        } else {
            ""
        }
        "$date | ${entry.sender} | ${entry.status.name}$errorPart"
    }

class ForwardingLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForwardingLogBinding
    private lateinit var viewModel: ForwardingLogViewModel
    private lateinit var adapter: ForwardingLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForwardingLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.forwarding_history)

        val logRepository = SharedPrefsForwardingLogRepository.create(this)
        viewModel = ViewModelProvider(
            this,
            ForwardingLogViewModelFactory(ForwardingLogGetUseCase(logRepository)),
        )[ForwardingLogViewModel::class.java]

        adapter = ForwardingLogAdapter()
        binding.logRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.logRecyclerView.adapter = adapter

        viewModel.log.observe(this) { entries ->
            val reversed = entries.reversed()
            adapter.submitList(reversed)
            binding.emptyText.visibility = if (reversed.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadLog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, MENU_SHARE, 0, R.string.share_log)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        MENU_SHARE -> {
            shareLog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun shareLog() {
        val entries = viewModel.log.value ?: emptyList()
        val text = formatLogAsText(entries.reversed())
        startActivity(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
        )
    }

    companion object {
        private const val MENU_SHARE = 1
    }
}

internal class ForwardingLogAdapter : RecyclerView.Adapter<ForwardingLogAdapter.ViewHolder>() {
    private var items: List<ForwardingLogEntry> = emptyList()

    fun submitList(list: List<ForwardingLogEntry>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forwarding_log_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timestampText: TextView = view.findViewById(R.id.entryTimestamp)
        private val senderText: TextView = view.findViewById(R.id.entrySender)
        private val statusText: TextView = view.findViewById(R.id.entryStatus)
        private val errorText: TextView = view.findViewById(R.id.entryErrorMessage)

        fun bind(entry: ForwardingLogEntry) {
            timestampText.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(Date(entry.timestamp))
            senderText.text = entry.sender
            statusText.text = when (entry.status) {
                ForwardingLogEntryStatus.FORWARDED -> itemView.context.getString(R.string.status_forwarded)
                ForwardingLogEntryStatus.FAILED -> itemView.context.getString(R.string.status_failed)
                ForwardingLogEntryStatus.SETUP_INCOMPLETE -> itemView.context.getString(R.string.status_setup_incomplete)
            }
            if (entry.status == ForwardingLogEntryStatus.FAILED && entry.errorMessage != null) {
                errorText.text = entry.errorMessage
                errorText.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
            }
        }
    }
}
