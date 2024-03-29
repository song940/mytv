package org.lsong.mytv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    private lateinit var player : ExoPlayer
    private var channels: List<Channel> = emptyList()
    private var currentChannelIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.initializeViews()
        this.setupPlayer()
        this.loadData()
    }
    private fun initializeViews(){
        val menuLayout = findViewById<LinearLayout>(R.id.menu_layout);
        menuLayout.visibility = View.GONE
    }
    private fun setupPlayer(){
        val playerView = findViewById<PlayerView>(R.id.player_view)
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            playerView.player = this
        }
        player.addListener(object: Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.d("onPlayerError", "${error.errorCode}: ${error.toString()}")
                if (error is ExoPlaybackException) {
                    handleExoPlayerError(error)
                }
                return super.onPlayerError(error)
            }
        })
    }
    fun handleExoPlayerError(error: ExoPlaybackException){
        Log.d("handleExoPlayerError", "${error.type}")
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
                // 处理源错误
            }
        }
    }
    private fun handleData(data: MyResponse) {
        val channelMap = data.channels.associateBy { it.name }
        val channelView = findViewById<ListView>(R.id.channel_list);
        channelView.setOnItemClickListener { parent, view, position, id ->
            this.playChannel(channels[position])
        }
        fun selectCategory(category: Category) {
            channels = category.channels.mapNotNull { channelMap[it] }
            channelView.adapter = ChannelAdapter(this, channels)
        }
        val categoryView = findViewById<RecyclerView>(R.id.category_list);
        categoryView.layoutManager = LinearLayoutManager(this)
        categoryView.adapter = CategoryAdapter(data.categories) { category ->
            // This code will be executed when a category item is clicked
            selectCategory(category)
        }
        selectCategory(data.categories[0])
        this.playChannel(channels[0])
    }
    private fun loadData() {
        RetrofitClient.getChannels(object : ChannelCallback {
            override fun onSuccess(response: MyResponse) {
                handleData(response)
            }
            override fun onError(error: String) {
                // 处理错误情况
            }
        })
    }
    private fun play(url: String){
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
    }
    fun showChannelInfo(channel: Channel) {
        val channelInfo = findViewById<LinearLayout>(R.id.channel_info)
        val channelTitleView = findViewById<TextView>(R.id.channel_title)
        channelTitleView.text = channel.name
        channelInfo.visibility = View.VISIBLE
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            channelInfo.visibility = View.GONE
        }
        handler.postDelayed(runnable, 3000)
    }
    private  fun playChannel(channel: Channel) {
        this.play(channel.sources[0].url)
        this.showChannelInfo(channel)
    }
    private fun changeChannel(direction: Int) {
        currentChannelIndex = (currentChannelIndex + direction + channels.size) % channels.size
        Log.d("MainActivity", "currentChannelIndex: $currentChannelIndex")
        val channel = channels[currentChannelIndex]
        this.playChannel(channel)
    }
    private fun showMenu() {
        val menuLayout = findViewById<LinearLayout>(R.id.menu_layout);
        if (menuLayout.visibility == View.GONE) {
            menuLayout.visibility = View.VISIBLE
        }
    }
    private fun hideMenu() {
        val menuLayout = findViewById<LinearLayout>(R.id.menu_layout);
        if (menuLayout.visibility == View.VISIBLE) {
            menuLayout.visibility = View.GONE
        }
    }
    private fun isMenuVisible(): Boolean {
        val menuLayout = findViewById<LinearLayout>(R.id.menu_layout);
        return menuLayout.visibility == View.VISIBLE
    }
    private fun toggleMenu() {
        if (isMenuVisible()) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val isLongPress = (event!!.eventTime - event.downTime) > 600
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (isLongPress) {
                    toggleMenu()
                } else {
                    showChannelInfo(channels[currentChannelIndex])
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                toggleMenu()
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                if (!isMenuVisible()) {
                    showMenu()
                    return true
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                if (isMenuVisible()) {
                    hideMenu()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!isMenuVisible()) {
                    changeChannel(-1)
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!isMenuVisible()) {
                    changeChannel(1)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onPause() {
        player.pause()
        super.onPause()
    }

    override fun onResume() {
        player.play()
        super.onResume()
    }

    override fun onStop() {
        player.stop()
        super.onStop()
    }
    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
