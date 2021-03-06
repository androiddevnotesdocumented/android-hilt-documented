/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.example.android.hilt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * Similarly to how the instance of ServiceLocator in the LogApplication class is used and initialized, to add a container that is attached to the app's lifecycle, we need to annotate the Application class with @HiltAndroidApp.
 *
 * @HiltAndroidApp triggers Hilt's code generation, including a base class for your application that can use dependency injection. The application container is the parent container for the app, which means that other containers can access the dependencies that it provides.
 */
@HiltAndroidApp
class LogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(object : DebugTree() {
            /**
             * Override [log] to modify the tag and add a "global tag" prefix to it. You can rename the String "global_tag_" as you see fit.
             */
            override fun log(
                priority: Int, tag: String?, message: String, t: Throwable?
            ) {
                super.log(priority, "global_tag_$tag", message, t)
            }

            /**
             * Override [createStackElementTag] to include a add a "method name" to the tag.
             */
            override fun createStackElementTag(element: StackTraceElement): String {
                return String.format(
                    "%s:%s",
                    element.methodName,
                    super.createStackElementTag(element)
                )
            }
        })

        // USAGE
        Timber.d("App created!")

    }
}
