package mexa.club.desktop_app.market.ui.maps

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.callback.CefQueryCallback
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

object JcefManager {
    private var cefApp: CefApp? = null
    private var cefClient: CefClient? = null
    private val initialized = AtomicBoolean(false)

    fun init() {
        if (initialized.get()) return
        var attempt = 0
        while (attempt < 3 && !initialized.get()) {
            attempt++
            try {
                val builder = CefAppBuilder()
                builder.setInstallDir(File("jcef-bundle"))
                builder.setProgressHandler(ConsoleProgressHandler())
                builder.addJcefArgs("--allow-file-access-from-files", "--no-proxy-server")
                // Google Maps barqaror renderi uchun software GL:
                // - disable-dev-shm-usage: /dev/shm kichikligida renderer crash/miltillashni oldini oladi
                // - enable-unsafe-swiftshader: yangi Chromium'da software WebGL fallback (Maps uchun)
                // - use-angle=swiftshader: ANGLE software render
                builder.addJcefArgs(
                    "--disable-dev-shm-usage",
                    "--enable-unsafe-swiftshader",
                    "--use-angle=swiftshader"
                )
                builder.cefSettings.windowless_rendering_enabled = false

                cefApp = builder.build()
                cefClient = cefApp?.createClient()

                initialized.set(true)
            } catch (e: Exception) {
                if (attempt < 3) {
                    try { Thread.sleep(2000) } catch (_: InterruptedException) {}
                }
            }
        }
        if (!initialized.get()) {
        }
    }

    fun createBrowser(url: String): CefBrowser? {
        if (!initialized.get()) return null
        return cefClient?.createBrowser(url, false, false)
    }

    fun dispose() {
        if (!initialized.get()) return
        try {
            cefClient?.dispose()
            cefApp?.dispose()
        } catch (e: Exception) {
        }
        initialized.set(false)
    }
}
