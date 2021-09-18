package burp

import java.util.*
import javax.swing.SwingUtilities

class BurpExtender(): IBurpExtender, IExtensionStateListener {

    companion object {
        const val version = "1.23"
    }

    override fun extensionUnloaded() {
        Utils.unloaded = true
    }

    override fun registerExtenderCallbacks(callbacks: IBurpExtenderCallbacks?) {
        callbacks!!.registerContextMenuFactory(OfferTurboIntruder())
        Utils.setBurpPresent(callbacks)
        callbacks.registerScannerCheck(Utils.witnessedWords)
        callbacks.registerExtensionStateListener(this)
        callbacks.setExtensionName("Turbo Intruder w/Haptyc")
        Utils.out("Loaded Turbo Intruder v$version")

        Utilities(callbacks, HashMap(), "Turbo Intruder")
        Utilities.globalSettings.registerSetting("font-size", 14);
        Utilities.globalSettings.registerSetting("line-numbers", true);
        Utilities.globalSettings.registerSetting("show-eol", false);
        Utilities.globalSettings.registerSetting("visible-whitespace", false);
        Utilities.globalSettings.registerSetting("Haptyc Helper", false);
        SwingUtilities.invokeLater(ConfigMenu())
    }
}
