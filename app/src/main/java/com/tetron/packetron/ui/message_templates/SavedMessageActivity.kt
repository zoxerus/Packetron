package com.tetron.packetron.ui.message_templates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.templates.MessageTemplate
import com.tetron.packetron.db.templates.TemplateViewModel


class SavedMessageActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var msgViewModel: TemplateViewModel
    private lateinit var adapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private var msgId: Long = -1

    private lateinit var addTemplateET: EditText
    private lateinit var addTemplateBTN: Button
    private lateinit var cancelTemplateBTN: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_message)

        addTemplateET = findViewById(R.id.et_add_message_template)
        addTemplateBTN = findViewById(R.id.bt_add_message_template)
        cancelTemplateBTN = findViewById(R.id.bt_cancel_message_template)

        recyclerView = findViewById(R.id.message_templates_rv)
        adapter = MessageAdapter(this, {
            val replyIntent = Intent()
            replyIntent.putExtra(getString(R.string.selected_message_template), it.message)
            setResult(RESULT_OK, replyIntent)
            finish()
        },
            { msg, action ->
                when (action) {
                    // action delete
                    0 -> {
                        msgViewModel.delete(msg)
                    }

                    // action edit
                    1 -> {
                        msgId = msg.id
                        addTemplateET.setText(msg.message)
                    }

                    // action copy
                    2 -> {
                        val clipboardManager =
                            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("text", msg.message)
                        clipboardManager.setPrimaryClip(clipData)
                    }
                }
            })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
        msgViewModel = ViewModelProvider(this).get(TemplateViewModel::class.java)
        msgViewModel.allMessages.observe(this, { messages ->
            messages?.let { adapter.setMessages(it) }
        })
        recyclerView.adapter = adapter

        addTemplateBTN.setOnClickListener(this)
        cancelTemplateBTN.setOnClickListener(this)
        registerForContextMenu(recyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.message_template_menu, menu)
        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mm_message_templates_action_delete -> {
                val deleteMessages = adapter.checkedMessages.toList()
                msgViewModel.deleteMany(deleteMessages)
                adapter.checkedMessages.clear()
            }
            R.id.mm_message_templates_action_delete_all -> {
                msgViewModel.deleteAll()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.bt_add_message_template -> {
                if (msgId == -1L) {
                    msgId = System.currentTimeMillis()
                }
                val msgText = addTemplateET.text.toString()
                val msg = MessageTemplate(id = msgId)
                msg.message = msgText
                msgViewModel.insert(msg)
                addTemplateET.text = null
                msgId = -1L
            }

            R.id.bt_cancel_message_template -> {
                addTemplateET.text = null
                msgId = -1L
            }
        }
    }
}