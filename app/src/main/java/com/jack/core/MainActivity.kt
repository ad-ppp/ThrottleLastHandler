package com.jack.core

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.jack.core.util.ThrottleLastHandler
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), ThrottleLastHandler.ThrottleLastDelayFactory,
    ThrottleLastHandler.OnThrottleLastResult<Int> {
    companion object {
        const val TAG = "Activity.MainActivity"
    }

    private val scheduler = Executors.newScheduledThreadPool(1)
    private val throttleLastHandler = ThrottleLastHandler<Int>(Looper.getMainLooper(), this, this)
    private var count = 1
    private var delayTime = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scheduler.scheduleAtFixedRate({
            throttleLastHandler.onSync(count++)
        }, 0, 100, TimeUnit.MILLISECONDS)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                delayTime = 100 * progress // mill
                textView.text = "set delayed $delayTime mill"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

        })
    }

    override fun delayInMill(): Int = delayTime

    override fun onThrottleLast(t: Int) {
        Log.i(TAG, "onThrottleLast $t")
    }

    override fun onDestroy() {
        super.onDestroy()
        throttleLastHandler.close()
        scheduler.shutdown()
    }
}
