/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.navigationsample

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation

/**
 * Shows the main title screen.
 */
class TitleScreen : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // 获取布局文件
        val view = inflater.inflate(R.layout.fragment_title_screen, container, false)

        // 在布局文件中找到 play_btn ，绑定事件
        view.findViewById<Button>(R.id.play_btn).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_title_screen_to_register)
        }
        // 在布局文件中找到 leaderboard_btn ，绑定事件
        view.findViewById<Button>(R.id.leaderboard_btn).setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_title_screen_to_leaderboard)
        }
        // 将布局文件展示到这个 TitleScreen上，  tools:context="com.example.android.navigationsample.TitleScreen"
        // 这个工具是做什么用的？见：R.layout.fragment_title_screen中的注释
        return view
    }
}
