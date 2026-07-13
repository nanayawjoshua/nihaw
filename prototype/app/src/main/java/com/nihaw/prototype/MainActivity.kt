package com.nihaw.prototype

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var inputText: EditText
    private lateinit var resultText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var translateButton: Button

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputText = findViewById(R.id.inputText)
        resultText = findViewById(R.id.resultText)
        progressBar = findViewById(R.id.progressBar)
        translateButton = findViewById(R.id.translateButton)

        translateButton.setOnClickListener { translate() }
        findViewById<Button>(R.id.sample1Button).setOnClickListener { loadSample1() }
        findViewById<Button>(R.id.sample2Button).setOnClickListener { loadSample2() }
    }

    private fun translate() {
        val text = inputText.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter Chinese text", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        resultText.visibility = android.view.View.GONE

        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { translation ->
                        progressBar.visibility = android.view.View.GONE
                        resultText.visibility = android.view.View.VISIBLE
                        resultText.text = translation
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this, "Translate failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Model download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadSample1() {
        inputText.setText("欢迎光临 1688 批发平台\nMOQ 500 件\n价格: ¥15.80/件\n库存: 20000 件\n预计发货时间: 3-5 天")
    }

    private fun loadSample2() {
        inputText.setText("产品描述: 高品质不锈钢保温杯\n容量: 500ml\n颜色: 黑色/白色/蓝色\n材质: 304不锈钢\n请在下单前确认库存情况\n付款方式: 支付宝/微信支付")
    }

    override fun onDestroy() {
        super.onDestroy()
        translator.close()
    }
}
