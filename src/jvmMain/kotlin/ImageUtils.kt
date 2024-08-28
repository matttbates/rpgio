import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File

fun createBitmapFromFile(filepath: String): ImageBitmap? {
    val file = File(filepath)
    return if (file.exists()) {
        val inputStream = file.inputStream()
        return loadImageBitmap(inputStream)
    } else {
        null
    }
}