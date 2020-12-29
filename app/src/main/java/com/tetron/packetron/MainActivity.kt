package com.tetron.packetron

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.navigation.NavigationView
import com.tetron.packetron.ui.ConnectionViewModel
import com.tetron.packetron.ui.about.AboutFragment
import com.tetron.packetron.ui.tcp_client.TCPClientFragment
import com.tetron.packetron.ui.tcp_server.TCPServerFragment
import com.tetron.packetron.ui.udp_send_receive.UDPSendReceiveFragment
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.system.exitProcess


const val LOG_TAG = "Main Activity"
const val UDP_FRAGMENT_TAG = "UDP Sender Receiver"
const val TCP_SERVER_FRAGMENT_TAG = "TCP Server"
const val TCP_CLIENT_FRAGMENT_TAG = "TCP Client"
const val ABOUT_FRAGMENT_TAG = "About"


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var adView: AdView

    private var udpSendReceiveFragment: UDPSendReceiveFragment? = null
    private var tcpServerFragment: TCPServerFragment? = null
    private var tcpClientFragment: TCPClientFragment? = null
    private var aboutFragment: AboutFragment? = null

    private var backPressedTime: Long = 0
    private lateinit var drawerLayout: DrawerLayout

    private lateinit var connectionViewModel: ConnectionViewModel


    // Determine the screen width (less decorations) to use for the ad width.
    // If the ad hasn't been laid out, default to the full screen width.
/*    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = ad_container.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)


        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        connectionViewModel = ViewModelProvider(this).get(ConnectionViewModel::class.java)

        udpSendReceiveFragment =
            UDPSendReceiveFragment.newInstance(connectionViewModel)

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, udpSendReceiveFragment!!, UDP_FRAGMENT_TAG)
            .addToBackStack(UDP_FRAGMENT_TAG)
            .commit()

        navView.setNavigationItemSelectedListener(this)
/*        if (!BuildConfig.DEBUG) {
            MobileAds.initialize(this) {}
            adView = AdView(this)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adView.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    adView.visibility = View.GONE
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }

                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdLeftApplication() {
                    // Code to be executed when the user has left the app.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }
            }
        }
        else{
            // Initialize the Mobile Ads SDK.
            MobileAds.initialize(this) { }

            adView = AdView(this)
            ad_container.addView(adView)
            adView.adUnitId = getString(R.string.TEST_AD_UNIT_ID)

            adView.adSize = adSize

            // Create an ad request. Check your logcat output for the hashed device ID to
            // get test ads on a physical device, e.g.,
            // "Use AdRequest.Builder.addTestDevice("ABCDE0123") to get test ads on this device."
            val adRequest = AdRequest
                .Builder().build()

            // Start loading the ad in the background.
            adView.loadAd(adRequest)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adView.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(errorCode: Int) {
                    adView.visibility = View.GONE
                }

                override fun onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }

                override fun onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                }

                override fun onAdLeftApplication() {
                    // Code to be executed when the user has left the app.
                }

                override fun onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }
            }
        }*/


    }

    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            this.finishAffinity()
            exitProcess(0)
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "Press again to exit ", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_view_local_ip -> {
                val fmd = ShowIpDialog()
                fmd.setIpAddress(getInetAddress(this).toString().removePrefix("/"))
                fmd.showNow(supportFragmentManager, "IP Dialog")
            }
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.message_templates -> {
/*                val templateIntent = Intent(this, SavedMessageActivity::class.java)
                startActivity(templateIntent)*/
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun getInetAddress(context: Context): InetAddress {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        return intToInetAddress(wifiManager.dhcpInfo.ipAddress)
    }

    fun intToInetAddress(hostAddress: Int): InetAddress {
        val addressBytes = byteArrayOf(
            (0xff and hostAddress).toByte(),
            (0xff and (hostAddress shr 8)).toByte(),
            (0xff and (hostAddress shr 16)).toByte(),
            (0xff and (hostAddress shr 24)).toByte()
        )
        return try {
            InetAddress.getByAddress(addressBytes)
        } catch (e: UnknownHostException) {
            throw AssertionError()
        }
    }

    class ShowIpDialog : DialogFragment() {
        private var ipAdrs: String = ""
        fun setIpAddress(ip: String) {
            this.ipAdrs = ip
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)

            builder.setMessage(ipAdrs)
                .setNegativeButton("OK", null)
                .setTitle("IP Address")
            return builder.create()
        }


    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragmentTag: String
        val fragment: Fragment?
        when (item.itemId) {
            R.id.nav_tcp_server -> {
                fragmentTag = TCP_SERVER_FRAGMENT_TAG
                title = fragmentTag
                if (tcpServerFragment == null) {
                    tcpServerFragment =
                        TCPServerFragment.newInstance(connectionViewModel)
                }
                fragment = tcpServerFragment
            }
            R.id.nav_tcp_client -> {
                fragmentTag = TCP_CLIENT_FRAGMENT_TAG
                title = fragmentTag
                if (tcpClientFragment == null) {
                    tcpClientFragment =
                        TCPClientFragment.newInstance(connectionViewModel)
                }
                fragment = tcpClientFragment
            }
            R.id.about_us -> {
                fragmentTag = ABOUT_FRAGMENT_TAG
                title = fragmentTag
                if (aboutFragment == null) {
                    aboutFragment =
                        AboutFragment()
                }
                fragment = aboutFragment
            }
            else -> {
                fragmentTag = UDP_FRAGMENT_TAG
                title = fragmentTag
                fragment = udpSendReceiveFragment
            }
        }
        val inputFragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (inputFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment!!, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit()
        } else {
            if (!fragment!!.isVisible) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment, fragmentTag)
                    .commit()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
