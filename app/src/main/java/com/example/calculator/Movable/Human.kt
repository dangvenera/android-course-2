import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

open class Human(
    private var fullName: String,
    private var age: Int,
    override var speed: Double
) : Movable {

    override var x: Double = 0.0
    override var y: Double = 0.0

    override fun move(dt: Double) {
        val angle = Random.nextDouble(0.0, 2 * PI)
        x += speed * dt * cos(angle)
        y += speed * dt * sin(angle)
    }

    fun getName(): String = fullName
    fun setName(newName: String) { fullName = newName }

    fun getAge(): Int = age
    fun setAge(newAge: Int) { age = newAge }
}