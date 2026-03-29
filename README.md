# Пастухов Александр Андреевич

**Группа:** ИКС-432

---

# RandomWalk and SimpleCalculator

## 📘 Часть 1. Модель Random Walk

В данной программе реализована **модель случайного блуждания (Random Walk)**.
Для метода `move()` используется следующая логика изменения координат объекта.

### Формулы движения

x(t + Δt) = x(t) + v * Δt * cos(θ)
y(t + Δt) = y(t) + v * Δt * sin(θ)

где:

* **v** — скорость,
* **Δt** — шаг по времени,
* **θ** — случайный угол, равномерно распределённый в [0, 2π].

---

### 🧩 Первичный конструктор

Объявляется непосредственно после имени класса и используется для инициализации свойств объекта при создании.

```kotlin
class Person(val name: String, var age: Int) {
    init {
        println("Создан объект Person: $name, $age")
    }
}
```

---

### 🔁 Вторичный конструктор

Позволяет задать альтернативные варианты инициализации объекта.

```kotlin
constructor(name: String, age: Int, city: String) : this(name, age) {
    println("Дополнительный конструктор: $name, $age, $city")
}
```

---

### 📂 Структура модуля Random Walk

* **Movable.kt** — интерфейс, описывающий объекты, которые могут двигаться (свойства `x`, `y`, `speed`, метод `move()`).
* **Human.kt** — класс `Human`, реализующий `Movable`, двигается случайно.
* **Driver.kt** — наследник `Human`, двигается по оси X.
* **Main.kt** — запускает симуляцию движения с многопоточностью.

---

## 🧮 Часть 2. Приложение "Калькулятор" (ПР4)

### 📱 Описание

Приложение **Calculator** реализовано в среде Android Studio на языке **Kotlin**.
Функционал — базовый калькулятор, способный выполнять арифметические операции:
**сложение, вычитание, умножение, деление**, а также корректно обрабатывать выражения.

### ⚙️ Основная логика

* Получение выражения из `TextView`
* Проверка корректности ввода
* Замена специальных символов (`×`, `÷`) на `*`, `/` с помощью `replace()`
* Расчёт выражения и вывод результата
* Обработка ошибок (например, деление на ноль)

### 🧠 Ключевые элементы

* `EditText` и `TextView` для отображения ввода и результата
* `Button` для каждой операции (`+`, `-`, `×`, `÷`, `C`, `=` и цифры 0–9)
* `onClickListener` для обработки нажатий
* `ExpressionEvaluator` или ручной парсер для вычислений

### 🗂️ Основные файлы

* **MainActivity.kt** — главный экран
* **activity_main.xml** — интерфейс калькулятора
* **AndroidManifest.xml** — описание Activity приложения

---

## 🧩 Часть 3. Рефакторинг и разделение по Activity

### 🔄 Цель

Реализовать **разделение функционала по Activity** и создание “хаба” для переходов.
Главная `MainActivity` теперь содержит кнопки перехода к отдельным экранам.

### 🧭 Реализация

* **MainActivity.kt** — “меню навигации” (hub), содержит кнопки:

  * `Calculator`
  * `Player`
  * `Location`
  * `Telephony`
  * `Sockets`
  * `Views`

* **CalculatorActivity.kt** — перенесён весь функционал калькулятора

* **AndroidManifest.xml** — обновлён с корректными ссылками на новые Activity

  ```xml
  <activity
            android:name=".CalculatorActivity"
            android:exported="false"
            android:label="@string/title_activity_calculator_activity.kt"
            android:theme="@style/Theme.Calculator" />
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
  ```

### 🎨 Внешний вид

Главный экран (`MainActivity`) оформлен в виде сетки кнопок (2 ряда по 3).
Используется фиолетовая цветовая схема, закруглённые углы (`rounded_button.xml`), и равномерное размещение элементов.  

---