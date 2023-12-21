package com.example.shootthemall

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.shootthemall.databinding.ActivityResultsBinding

class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(false)

        val score = intent.getIntExtra("points",0)

        binding.textPoints.text =binding.textPoints.text
            .replaceRange(15,15, score.toString())

        binding.buttonSave.setOnClickListener {
            val data = Intent()
            data.putExtra("username", binding.editUsername.text.toString())
            data.putExtra("score", score)
            setResult(RESULT_OK, data)
            finish()
        }

        binding.buttonCancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}