package com.oscar.smartcontrol.cast

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class DlnaRenderer(
    val name: String,
    val controlUrl: String,
    val host: String
)

/**
 * Zero-dependency DLNA/UPnP media caster + local HTTP Range server.
 * Streams local photos and videos directly to any DLNA-capable Smart TV.
 */
class MediaCastManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _renderers = MutableStateFlow<List<DlnaRenderer>>(emptyList())
    val renderers: StateFlow<List<DlnaRenderer>> = _renderers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _castStatus = MutableStateFlow<String?>(null)
    val castStatus: StateFlow<String?> = _castStatus.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private val serverRunning = AtomicBoolean(false)

    @Volatile
    private var currentUri: Uri? = null

    @Volatile
    private var currentMime: String = "application/octet-stream"

    @Volatile
    private var lastRenderer: DlnaRenderer? = null

    fun discoverRenderers(onDone: (() -> Unit)? = null) {
        if (_isScanning.value) return
        scope.launch {
            _isScanning.value = true
            val found = linkedMapOf<String, DlnaRenderer>()
            try {
                val socket = DatagramSocket().apply {
                    soTimeout = 2200
                    broadcast = true
                }
                val targets = listOf(
                    "urn:schemas-upnp-org:service:AVTransport:1",
                    "urn:schemas-upnp-org:device:MediaRenderer:1"
                )
                val group = InetAddress.getByName("239.255.255.250")
                for (st in targets) {
                    val query = (
                        "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: 239.255.255.250:1900\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: $st\r\n\r\n"
                        ).toByteArray()
                    socket.send(DatagramPacket(query, query.size, group, 1900))
                }

                val buf = ByteArray(4096)
                val endAt = System.currentTimeMillis() + 2400L
                while (System.currentTimeMillis() < endAt) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        socket.receive(packet)
                        val msg = String(packet.data, 0, packet.length)
                        val location = extractHeader(msg, "LOCATION") ?: continue
                        if (!found.containsKey(location)) {
                            parseRendererDescription(location)?.let { renderer ->
                                found[location] = renderer
                                _renderers.value = found.values.toList()
                            }
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
                socket.close()
            } catch (_: Exception) {
            } finally {
                _isScanning.value = false
                onDone?.invoke()
            }
        }
    }

    fun castUri(uri: Uri, renderer: DlnaRenderer? = null) {
        scope.launch {
            _castStatus.value = "Preparando transmisión..."
            val target = renderer ?: lastRenderer ?: _renderers.value.firstOrNull()
            if (target == null) {
                // Quick discovery before failing
                discoverRenderersSync()
            }
            val finalTarget = renderer ?: lastRenderer ?: _renderers.value.firstOrNull()
            if (finalTarget == null) {
                _castStatus.value = "No se encontró TV DLNA. Probá 'Espejar pantalla' o abrí VLC/Kodi en la TV."
                return@launch
            }
            lastRenderer = finalTarget

            val mime = context.contentResolver.getType(uri) ?: "video/mp4"
            currentUri = uri
            currentMime = mime

            val port = ensureHttpServer()
            val localIp = getLocalIpv4Address()
            if (port <= 0 || localIp == null) {
                _castStatus.value = "Error obteniendo IP local Wi-Fi"
                return@launch
            }

            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            val mediaUrl = "http://$localIp:$port/media.$ext"

            val setOk = sendSoapAction(
                controlUrl = finalTarget.controlUrl,
                action = "SetAVTransportURI",
                body = """
                    <u:SetAVTransportURI xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <CurrentURI>${escapeXml(mediaUrl)}</CurrentURI>
                        <CurrentURIMetaData></CurrentURIMetaData>
                    </u:SetAVTransportURI>
                """.trimIndent()
            )
            if (!setOk) {
                _castStatus.value = "La TV (${finalTarget.name}) rechazó el archivo"
                return@launch
            }

            sendSoapAction(
                controlUrl = finalTarget.controlUrl,
                action = "Play",
                body = """
                    <u:Play xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                        <Speed>1</Speed>
                    </u:Play>
                """.trimIndent()
            )
            _castStatus.value = "Transmitiendo a ${finalTarget.name}"
        }
    }

    fun stopCast() {
        val target = lastRenderer ?: _renderers.value.firstOrNull() ?: return
        scope.launch {
            sendSoapAction(
                controlUrl = target.controlUrl,
                action = "Stop",
                body = """
                    <u:Stop xmlns:u="urn:schemas-upnp-org:service:AVTransport:1">
                        <InstanceID>0</InstanceID>
                    </u:Stop>
                """.trimIndent()
            )
            _castStatus.value = "Transmisión detenida"
        }
    }

    fun clearStatus() {
        _castStatus.value = null
    }

    fun shutdown() {
        serverRunning.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }

    private suspend fun discoverRenderersSync() = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, DlnaRenderer>()
        try {
            val socket = DatagramSocket().apply {
                soTimeout = 1800
                broadcast = true
            }
            val query = (
                "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 2\r\n" +
                    "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n\r\n"
                ).toByteArray()
            val group = InetAddress.getByName("239.255.255.250")
            socket.send(DatagramPacket(query, query.size, group, 1900))

            val buf = ByteArray(4096)
            val endAt = System.currentTimeMillis() + 2000L
            while (System.currentTimeMillis() < endAt) {
                try {
                    val packet = DatagramPacket(buf, buf.size)
                    socket.receive(packet)
                    val msg = String(packet.data, 0, packet.length)
                    val location = extractHeader(msg, "LOCATION") ?: continue
                    if (!found.containsKey(location)) {
                        parseRendererDescription(location)?.let { found[location] = it }
                    }
                } catch (_: Exception) {
                    break
                }
            }
            socket.close()
            if (found.isNotEmpty()) {
                _renderers.value = found.values.toList()
            }
        } catch (_: Exception) {
        }
    }

    private fun ensureHttpServer(): Int {
        serverSocket?.let {
            if (!it.isClosed && serverRunning.get()) return it.localPort
        }
        return try {
            val ss = ServerSocket(0)
            serverSocket = ss
            serverRunning.set(true)
            scope.launch {
                while (serverRunning.get() && !ss.isClosed) {
                    try {
                        val client = ss.accept()
                        scope.launch { handleHttpClient(client) }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            ss.localPort
        } catch (_: Exception) {
            -1
        }
    }

    private fun handleHttpClient(socket: Socket) {
        socket.use { s ->
            try {
                val input = s.getInputStream().bufferedReader()
                val requestLine = input.readLine() ?: return
                var rangeHeader: String? = null
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter(':').trim()
                    }
                }

                val uri = currentUri ?: return
                val mime = currentMime
                val totalSize = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    it.length
                } ?: -1L

                val out = BufferedOutputStream(s.getOutputStream())
                val isHead = requestLine.startsWith("HEAD", ignoreCase = true)

                if (rangeHeader != null && totalSize > 0 && rangeHeader.startsWith("bytes=")) {
                    val parts = rangeHeader.removePrefix("bytes=").split("-")
                    val start = parts[0].toLongOrNull() ?: 0L
                    val end = parts.getOrNull(1)?.toLongOrNull() ?: (totalSize - 1)
                    val len = (end - start + 1).coerceAtLeast(0L)

                    val headers = "HTTP/1.1 206 Partial Content\r\n" +
                        "Content-Type: $mime\r\n" +
                        "Content-Length: $len\r\n" +
                        "Content-Range: bytes $start-$end/$totalSize\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Connection: close\r\n\r\n"
                    out.write(headers.toByteArray())
                    if (!isHead) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.skipFully(start)
                            copyLimited(stream, out, len)
                        }
                    }
                } else {
                    val lenHeader = if (totalSize > 0) "Content-Length: $totalSize\r\n" else ""
                    val headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: $mime\r\n" +
                        lenHeader +
                        "Accept-Ranges: bytes\r\n" +
                        "Connection: close\r\n\r\n"
                    out.write(headers.toByteArray())
                    if (!isHead) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BufferedInputStream(stream).copyTo(out, 16384)
                        }
                    }
                }
                out.flush()
            } catch (_: Exception) {
            }
        }
    }

    private fun InputStream.skipFully(bytesToSkip: Long) {
        var remaining = bytesToSkip
        val skipBuf = ByteArray(8192)
        while (remaining > 0) {
            val read = read(skipBuf, 0, minOf(remaining, skipBuf.size.toLong()).toInt())
            if (read <= 0) break
            remaining -= read
        }
    }

    private fun copyLimited(input: InputStream, out: BufferedOutputStream, limit: Long) {
        val buf = ByteArray(16384)
        var remaining = limit
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size.toLong()).toInt()
            val read = input.read(buf, 0, toRead)
            if (read <= 0) break
            out.write(buf, 0, read)
            remaining -= read
        }
    }

    private fun parseRendererDescription(locationUrl: String): DlnaRenderer? {
        return try {
            val url = URL(locationUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2000
                readTimeout = 2000
            }
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val friendlyName = Regex("<friendlyName>(.*?)</friendlyName>", RegexOption.DOT_MATCHES_ALL)
                .find(xml)?.groupValues?.get(1)?.trim() ?: url.host

            // Find AVTransport service block
            val serviceBlock = Regex(
                "<service>.*?AVTransport:1.*?</service>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
            ).find(xml)?.value ?: return null

            val controlPath = Regex("<controlURL>(.*?)</controlURL>", RegexOption.DOT_MATCHES_ALL)
                .find(serviceBlock)?.groupValues?.get(1)?.trim() ?: return null

            val controlUrl = URL(url, controlPath).toString()
            DlnaRenderer(name = friendlyName, controlUrl = controlUrl, host = url.host)
        } catch (_: Exception) {
            null
        }
    }

    private fun sendSoapAction(controlUrl: String, action: String, body: String): Boolean {
        return try {
            val envelope = """
                <?xml version="1.0" encoding="utf-8"?>
                <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                    <s:Body>
                        $body
                    </s:Body>
                </s:Envelope>
            """.trimIndent()

            val conn = (URL(controlUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                setRequestProperty("SOAPACTION", "\"urn:schemas-upnp-org:service:AVTransport:1#$action\"")
            }
            conn.outputStream.use { it.write(envelope.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun extractHeader(raw: String, name: String): String? {
        for (line in raw.lines()) {
            if (line.startsWith("$name:", ignoreCase = true)) {
                return line.substringAfter(':').trim()
            }
        }
        return null
    }

    private fun getLocalIpv4Address(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
