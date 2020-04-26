package com.tetron.packetron

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationView
import com.tetron.packetron.ui.ConnectionViewModel
import com.tetron.packetron.ui.tcp_client.TCPClientFragment
import com.tetron.packetron.ui.tcp_server.TCPServerFragment
import com.tetron.packetron.ui.udp_send_receive.UDPSendReceiveFragment
import java.net.InetAddress
import java.net.UnknownHostException


const val LOG_TAG = "Main Activity"
const val UDP_FRAGMENT_TAG = "UDP Sender Receiver"
const val TCP_SERVER_FRAGMENT_TAG = "TCP Server"
const val TCP_CLIENT_FRAGMENT_TAG = "TCP Client"

class MainActivity : AppCompatActivity() {

    private var udpSendReceiveFragment: UDPSendReceiveFragment? = null
    private var tcpServerFragment: TCPServerFragment? = null
    private var tcpClientFragment: TCPClientFragment? = null

    private val connectionViewModel: ConnectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        Log.e(LOG_TAG, "Main Activity Created")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)


        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }


        udpSendReceiveFragment =
            UDPSendReceiveFragment.newInstance(connectionViewModel)

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, udpSendReceiveFragment!!, UDP_FRAGMENT_TAG)
            .commit()


        navView.setNavigationItemSelectedListener {
            drawerLayout.closeDrawer(GravityCompat.START)

            val fragmentTag: String
            val fragment: Fragment?
            when (it.itemId) {
                R.id.nav_tcp_server -> {
                    fragmentTag = TCP_SERVER_FRAGMENT_TAG
                    if (tcpServerFragment == null) tcpServerFragment =
                        TCPServerFragment.newInstance(connectionViewModel)
                    fragment = tcpServerFragment
                }
                R.id.nav_tcp_client -> {
                    fragmentTag = TCP_CLIENT_FRAGMENT_TAG
                    if (tcpClientFragment == null) tcpClientFragment =
                        TCPClientFragment.newInstance(connectionViewModel)
                    fragment = tcpClientFragment
                }
                R.id.nav_udp -> {
                    fragmentTag = UDP_FRAGMENT_TAG
                    if (udpSendReceiveFragment == null) udpSendReceiveFragment =
                        UDPSendReceiveFragment.newInstance(connectionViewModel)
                    fragment = udpSendReceiveFragment
                }
                else -> {
                    fragmentTag = UDP_FRAGMENT_TAG
                    if (udpSendReceiveFragment == null) udpSendReceiveFragment =
                        UDPSendReceiveFragment.newInstance(connectionViewModel)
                    fragment = udpSendReceiveFragment
                }
            }
            val inputFragment: Fragment? = supportFragmentManager.findFragmentByTag(fragmentTag)
            Handler().postDelayed({
                if (inputFragment == null) {
                    supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.nav_host_fragment, fragment!!, fragmentTag)
                        .addToBackStack(fragmentTag)
                        .commit()

                } else {
                    //findViewById<View>(R.id.fragment_container_layout).visibility = View.INVISIBLE
                    supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.nav_host_fragment, inputFragment, fragmentTag)
                        .commit()
                }

            }, 300)


            true
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
                fmd.setIpAddress(getIpAddress(this)!!)
                fmd.showNow(supportFragmentManager, "IP Dialog")
            }
            R.id.action_settings -> {
                val settingsIntent: Intent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onPause() {
        super.onPause()
        Log.e(LOG_TAG, "Main Activity Paused")
    }

    override fun onResume() {
        super.onResume()
        Log.e(LOG_TAG, "Main Activity Resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(LOG_TAG, "Main Activity Destroyed")
    }

    fun getIpAddress(context: Context): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        var ipAddress: String =
            intToInetAddress(wifiManager.dhcpInfo.ipAddress).toString()
        ipAddress = ipAddress.substring(1)
        return ipAddress
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
        var ipAdrs: String = ""
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


}
