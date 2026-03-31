package com.sen.di_pan.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSString
import platform.Foundation.writeToURL
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun shareCsvFile(content: String, fileName: String, context: Any?) {
    val tempDir = NSTemporaryDirectory()
    val filePath = tempDir + fileName
    val url = NSURL.fileURLWithPath(filePath)
    
    val nsContent = NSString.create(string = content)
    nsContent.writeToURL(
        url = url,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null
    )

    val activityViewController = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null
    )

    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController
    
    rootViewController?.presentViewController(
        activityViewController,
        animated = true,
        completion = null
    )
}
