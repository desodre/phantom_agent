package com.example.phantom_agent

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhantomServer {

  @Test
  fun startServer() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val device = UiDevice.getInstance(instrumentation)
    val clientExecutor = Executors.newCachedThreadPool()

    ServerSocket(PORT).use { serverSocket ->
      while (true) {
        val clientSocket = serverSocket.accept()
        clientExecutor.execute { handleClient(clientSocket, device) }
      }
    }
  }

  private fun handleClient(clientSocket: Socket, device: UiDevice) {
    var writer: BufferedWriter? = null

    try {
      val reader =
        BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
      writer = BufferedWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8))

      val payload = reader.readLine() ?: throw IllegalArgumentException("Empty request")
      val request = JSONObject(payload)
      val action = request.getString("action")

      val response =
        when (action) {
          "dumpWindow" -> {
            val output = ByteArrayOutputStream()
            device.dumpWindowHierarchy(output)
            JSONObject().put("status", "success").put("xml", output.toString(Charsets.UTF_8.name()))
          }
          "clickByText" -> {
            val text = request.getString("text")
            val target = device.findObject(UiSelector().text(text))
            if (target.exists() && target.click()) {
              JSONObject().put("status", "success")
            } else {
              JSONObject().put("status", "error").put("message", "Element not found")
            }
          }
          else -> JSONObject().put("status", "error").put("message", "Unknown action: $action")
        }

      writer.write(response.toString())
      writer.newLine()
      writer.flush()
    } catch (e: Exception) {
      val error =
        JSONObject()
          .put("status", "error")
          .put("message", e.message ?: e::class.java.simpleName)

      writer?.runCatching {
        write(error.toString())
        newLine()
        flush()
      }
    } finally {
      runCatching { clientSocket.close() }
    }
  }

  private companion object {
    const val PORT = 9008
  }
}
