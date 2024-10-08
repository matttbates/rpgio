package server

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class FileIO {

    fun readTextFile(fileName: String): String? {
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val resourcesPath = Paths.get(projectDirAbsolutePath, fileName)
        return try{
            Files.readString(resourcesPath)
        }catch (e: Exception){
            null
        }
    }

    fun writeTextFile(fileName: String, content: String) {
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val path = Paths.get(projectDirAbsolutePath, fileName)
        if(!Files.exists(path)){
            Files.createFile(path)
        }
        Files.write(path, content.toByteArray())
    }

    fun createBitmapFromFile(filepath: String): ImageBitmap? {
        val file = File(filepath)
        return if (file.exists()) {
            val inputStream = file.inputStream()
            return loadImageBitmap(inputStream)
        } else {
            null
        }
    }

    fun getAllTiles(): List<String>{
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val resourcesPath = Paths.get(projectDirAbsolutePath, "/src/jvmMain/resources/common.tiles")
        val list = arrayListOf<String>()
        Files.walk(resourcesPath)
            .filter { item -> Files.isRegularFile(item) }
            .filter { item -> item.toString().endsWith(".png") }
            .forEach { item ->
                list.add("src/jvmMain/resources/common.tiles/${item.toFile().name}")
            }
        return list
    }

}