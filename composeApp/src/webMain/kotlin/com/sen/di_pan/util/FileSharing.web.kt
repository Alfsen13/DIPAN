package com.sen.di_pan.util

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual fun shareCsvFile(content: String, fileName: String, context: Any?) {
    val blob = Blob(arrayOf(content), BlobPropertyBag(type = "text/csv"))
    val url = URL.createObjectURL(blob)
    
    val anchor = document.createElement("a") as HTMLAnchorElement
    anchor.href = url
    anchor.download = fileName
    anchor.click()
    
    URL.revokeObjectURL(url)
}
