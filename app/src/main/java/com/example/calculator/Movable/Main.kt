fun main() {
    val people = listOf(
        Human(fullName = "Вера Геннадьевна Дроздова", age = 26, speed = 2.0),
        Human(fullName = "Андреев Андрей Валерьевич", age = 27, speed = 2.5),
        Human(fullName = "Пастухов Александр Андреевич", age = 19, speed = 1.3),
        Driver(fullName = "Жолар Тимур Батрудинович (водитель)", age = 31, speed = 3.2)
    )

    val simulationTime = 5.0
    val step = 1.0

    println("Начало симуляции движения (${people.size} объектов)\n")

    val threads = mutableListOf<Thread>()

    for (person in people) {
        val t = Thread {
            var time = 0.0
            while (time <= simulationTime) {
                person.move(step)
                println("[${Thread.currentThread().name}] ${person.getName()} → (%.2f, %.2f)"
                    .format(person.x, person.y))
                time += step
                Thread.sleep((300..900).random().toLong())
            }
        }
        t.name = person.getName()
        threads.add(t)
    }

    // запускаем все потоки
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    println("\nСимуляция завершена!")
    println("\nФинальные позиции:")

    println("=".repeat(60))
    for (person in people) {
        println("%-40s (%.2f, %.2f)".format(person.getName(), person.x, person.y))
    }
    println("=".repeat(60))
}