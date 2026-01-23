package com.cherry.doc

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: App
 * Author: Victor
 * Date: 2023/10/16 17:26
 * Description: 
 * -----------------------------------------------------------------
 */

class App : Application() {

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PDFBoxResourceLoader.init(applicationContext)
    }
}