package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class SocketsActivity : AppCompatActivity() {

    private lateinit var tvSockets: TextView

    private val serverIp = "192.168.0.14"
    private val serverPort = 5555

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sockets)

        tvSockets = findViewById(R.id.tvSockets)

        findViewById<Button>(R.id.btnStartZmq).setOnClickListener {
            tvSockets.text = "Подключаемся к TCP: $serverIp:$serverPort ..."
            Thread { runClientOnce() }.start()
        }
    }

    private fun runClientOnce() {
        val request = "Hello from Android!"
        try {
            ZContext().use { ctx ->
                val socket = ctx.createSocket(SocketType.REQ)
                socket.connect("tcp://$serverIp:$serverPort")

                socket.send(request.toByteArray(ZMQ.CHARSET), 0)
                val replyBytes = socket.recv(0)
                val reply = String(replyBytes, ZMQ.CHARSET)

                runOnUiThread {
                    tvSockets.text = "Отправлено: $request\nПолучено: $reply"
                }

                socket.close()
            }
        } catch (e: Exception) {
            runOnUiThread {
                tvSockets.text = "Ошибка: ${e.message}"
            }
        }
    }
}