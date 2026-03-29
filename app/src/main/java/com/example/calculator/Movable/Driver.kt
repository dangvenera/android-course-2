class Driver(
    fullName: String,
    age: Int,
    speed: Double
) : Human(fullName, age, speed) {

    override fun move(dt: Double) {
        x += speed * dt // движение по оси X
    }
}
