package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.DiffType
import com.example.util.DiffUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WebWatch", appName)
    }

    @Test
    fun `test diff calculation additions and deletions`() {
        val oldHtml = "<html>\n<body>\n<h1>Title</h1>\n<p>Old content</p>\n</body>\n</html>"
        val newHtml = "<html>\n<body>\n<h1>Title 2</h1>\n<p>New content</p>\n<div>Extra</div>\n</body>\n</html>"

        val diff = DiffUtils.computeDiff(oldHtml, newHtml)
        assertTrue(diff.hasChanges)
        assertTrue(diff.additions > 0)
        assertTrue(diff.deletions > 0)
    }

    @Test
    fun `test normalize html strips scripts`() {
        val raw = "<html><head><script>var x = 123;</script></head><body><h1>Hello</h1></body></html>"
        val normalized = DiffUtils.normalizeHtml(raw, stripScripts = true, formatTags = true)
        assertTrue(!normalized.contains("var x = 123"))
        assertTrue(normalized.contains("Hello"))
    }
}
