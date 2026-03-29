package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class SerializationActivity : AppCompatActivity() {

    private lateinit var tv: TextView
    private var running = false

    private val serverIp = "192.168.0.14"
    private val port = 5555

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_serialization)

        tv = findViewById(R.id.tvSerialization)

        findViewById<Button>(R.id.btnStartJson).setOnClickListener {
            if (!running) {
                running = true
                tv.text = "Отправка каждую 1s..."
                Thread { sendLoop() }.start()
            }
        }

        findViewById<Button>(R.id.btnStopJson).setOnClickListener {
            running = false
            tv.text = "Остановлено"
        }
    }

    private fun sendLoop() {
        var counter = 0
        ZContext().use { ctx ->
            val socket = ctx.createSocket(SocketType.REQ)
            socket.connect("tcp://$serverIp:$port")

            while (running) {
                val payload = TelemetryBuilder.buildPacketJson(this)
                if (payload == null) {
                    runOnUiThread { tv.text = "Нет Location (или нет permission)" }
                    Thread.sleep(1000)
                    continue
                }

                socket.send(payload.toByteArray(ZMQ.CHARSET), 0)
                val reply = String(socket.recv(0), ZMQ.CHARSET)

                counter++
                runOnUiThread {
                    tv.text = "Отправлено пакетов: $counter\nОтвет сервера: $reply\nРазмер переданных данных (JSON): ${payload.length} байт"
                }

                Thread.sleep(1000)
            }

            socket.close()
        }
    }
}