package com.example.quickbase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import org.quick.base.Notify
import org.quick.base.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        titleTv.setOnClickListener {
            var i = cast<Long>()
        }
        noticeTv.setOnClickListener {
            Notify.Builder(123)
                .content(R.mipmap.ic_launcher_round, "标题", "这是内容")
//                .onClickListener { context, intent ->
//                    Log.e("notice", "点击")
//                }
//                .onCancelListener { context, intent ->
//                    Log.e("notice", "取消")
//                }
                .action()
//            Toast.Builder(R.layout.custom_toast)
//                .show{toast,vh->
//                    vh.setText(R.id.msgTv2,"这是新消息")
//                }
            Toast.show("这是消息内容")
        }

    }

    fun <T> cast(): T {
        var str = "120"
        return str.toInt() as T
    }
}
