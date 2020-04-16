package com.tetron.packetron

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.google.android.material.navigation.NavigationView
import com.tetron.packetron.ui.tcp_server.TCPServerFragment
import com.tetron.packetron.ui.udp_send_receive.UDPSendReceiveFragment
import java.net.InetAddress
import java.net.UnknownHostException


const val LOG_TAG = "Main Activity"
const val UDP_FRAGMENT_TAG = "UDP_Fragment"
const val TCP_SERVER_FRAGMENT_TAG = "TCP_Server_Fragment"
const val TCP_CLIENT_FRAGMENT_TAG = "UDP Receive"


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var udpSendReceiveFragment: UDPSendReceiveFragment? = null
    private var tcpServerFragment: TCPServerFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

/*        if (savedInstanceState != null) {
            //Restore the fragment's instance
            udpSendReceiveFragment = supportFragmentManager
                .getFragment(savedInstanceState, "myFragmentName") as UDPSendReceiveFragment
        }*/

        Log.e(LOG_TAG, "Main Activity Created")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)


        // val navController = findNavController(R.id.nav_host_fragment)


        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
/*        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_udp_send_receive, R.id.nav_udp_send, R.id.nav_udp_receive
            ), drawerLayout
        )*/


        //setupActionBarWithNavController(navController, appBarConfiguration)
        //navView.setupWithNavController(navController)

        udpSendReceiveFragment =
            UDPSendReceiveFragment.newInstance()

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, udpSendReceiveFragment!!, UDP_FRAGMENT_TAG)
            .commit()
        this.title = UDP_FRAGMENT_TAG
        navView.setNavigationItemSelectedListener {
            drawerLayout.closeDrawer(GravityCompat.START)

            val fragmentTag: String
            val fragment: Fragment?
            when (it.itemId) {
                R.id.nav_udp_send -> {
                    fragmentTag = TCP_SERVER_FRAGMENT_TAG
                    if (tcpServerFragment == null) tcpServerFragment =
                        TCPServerFragment.newInstance()
                    fragment = tcpServerFragment
                }
                else -> {
                    fragmentTag = UDP_FRAGMENT_TAG
                    if (udpSendReceiveFragment == null) udpSendReceiveFragment =
                        UDPSendReceiveFragment.newInstance()
                    fragment = udpSendReceiveFragment
                }
            }
            val inputFragment: Fragment? = supportFragmentManager.findFragmentByTag(fragmentTag)
            Handler().postDelayed({
                if (inputFragment == null) {
                    //findViewById<View>(R.id.fragment_container_layout).visibility = View.INVISIBLE
                    supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.nav_host_fragment, fragment!!, fragmentTag)
                        .addToBackStack(fragmentTag)
                        .commit()
                    this.title = fragmentTag

                } else {
                    //findViewById<View>(R.id.fragment_container_layout).visibility = View.INVISIBLE
                    supportFragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.nav_host_fragment, inputFragment, fragmentTag)
                        .commit()
                    this.title = fragmentTag
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
                val fmd = FireMissilesDialogFragment()
                fmd.setIpAddress(getIpAddress(this)!!)
                fmd.showNow(supportFragmentManager, "sesx")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        supportFragmentManager.putFragment(outState, "myFragmentName", udpSendReceiveFragment!!)
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

    class FireMissilesDialogFragment : DialogFragment() {
        var ipAdrs: String = ""
        fun setIpAddress(ip: String) {
            this.ipAdrs = ip
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(activity)

            builder.setMessage(ipAdrs)
                .setNegativeButton("OK", null)
            // Create the AlertDialog object and return it
            return builder.create()
        }

/*
        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            return activity?.let {
                // Use the Builder class for convenient dialog construction
                val builder = AlertDialog.Builder(it)
                builder.setMessage("HEll")
                    .setPositiveButton("aye",
                        DialogInterface.OnClickListener { dialog, id ->
                            // FIRE ZE MISSILES!
                        })
                    .setNegativeButton("OK",
                        DialogInterface.OnClickListener { dialog, id ->
                            // User cancelled the dialog
                        })
                // Create the AlertDialog object and return it
                builder.create()
            } ?: throw IllegalStateException("Activity cannot be null")
        }
*/
    }


}
