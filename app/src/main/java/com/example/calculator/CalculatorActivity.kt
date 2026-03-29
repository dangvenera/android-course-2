package com.example.calculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class CalculatorActivity : AppCompatActivity() {

    private lateinit var mainText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        mainText = findViewById(R.id.main_text)

        val btn0 = findViewById<Button>(R.id.btn_0)
        val btn1 = findViewById<Button>(R.id.btn_1)
        val btn2 = findViewById<Button>(R.id.btn_2)
        val btn3 = findViewById<Button>(R.id.btn_3)
        val btn4 = findViewById<Button>(R.id.btn_4)
        val btn5 = findViewById<Button>(R.id.btn_5)
        val btn6 = findViewById<Button>(R.id.btn_6)
        val btn7 = findViewById<Button>(R.id.btn_7)
        val btn8 = findViewById<Button>(R.id.btn_8)
        val btn9 = findViewById<Button>(R.id.btn_9)

        val btnPlus = findViewById<Button>(R.id.btn_plus)
        val btnMinus = findViewById<Button>(R.id.btn_minus)
        val btnMul = findViewById<Button>(R.id.btn_umnozenie)
        val btnDiv = findViewById<Button>(R.id.delenie)
        val btnDot = findViewById<Button>(R.id.btn_zapyataya)
        val btnAC = findViewById<Button>(R.id.AC)
        val btnDel = findViewById<Button>(R.id.btn_delete)
        val btnEq = findViewById<Button>(R.id.btn_ravno)

        val buttons = listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9,
            btnPlus, btnMinus, btnMul, btnDiv, btnDot)

        for (b in buttons) {
            b.setOnClickListener {
                mainText.append(b.text)
            }
        }

        btnAC.setOnClickListener { mainText.text = "" }

        btnDel.setOnClickListener {
            val str = mainText.text.toString()
            if (str.isNotEmpty()) {
                mainText.text = str.substring(0, str.length - 1)
            }
        }

        btnEq.setOnClickListener {
            val expr = mainText.text.toString()
            mainText.text = calculate(expr)
        }
    }

    private fun calculate(expression: String): String {
        if (expression.isEmpty()) return ""

        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<Char>()
        var currentNumber = ""

        for (c in expression) {
            if (c.isDigit() || c == '.') {
                currentNumber += c
            } else {
                if (currentNumber.isNotEmpty()) {
                    numbers.add(currentNumber.toDouble())
                    currentNumber = ""
                }
                if (c == '+' || c == '-' || c == 'x' || c == '÷') {
                    operators.add(c)
                }
            }
        }

        if (currentNumber.isNotEmpty()) {
            numbers.add(currentNumber.toDouble())
        }

        var i = 0
        while (i < operators.size) {
            val op = operators[i]
            if (op == 'x' || op == '÷') {
                val a = numbers[i]
                val b = numbers[i + 1]
                val res = if (op == 'x') a * b else if (b != 0.0) a / b else return "Деление на 0"
                numbers[i] = res
                numbers.removeAt(i + 1)
                operators.removeAt(i)
            } else {
                i++
            }
        }

        var result = numbers[0]
        for (j in operators.indices) {
            val op = operators[j]
            val b = numbers[j + 1]
            if (op == '+') result += b
            if (op == '-') result -= b
        }

        return if (result % 1 == 0.0) result.toInt().toString() else result.toString()
    }
}