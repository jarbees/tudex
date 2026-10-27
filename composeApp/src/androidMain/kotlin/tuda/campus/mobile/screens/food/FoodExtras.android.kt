package tuda.campus.mobile.screens.food

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import coil3.Canvas
import coil3.ImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import tuda.campus.mobile.components.SmallNavTitle

@Composable
actual fun HtmlTextView(
    navigateBack: () -> Unit,
    title: String,
    html: String
) {
    SmallNavTitle(
        title = title,
        backAction = navigateBack
    ) {
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
            update = { textView ->
                val spanned = html.parseAsHtml(
                    HtmlCompat.FROM_HTML_MODE_COMPACT,
                    CoilImageGetter(textView)
                )
                textView.text = spanned
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


// Adopted from https://github.com/Commit451/coil-imagegetter/blob/main/coilimagegetter/src/main/java/com/commit451/coilimagegetter/CoilImageGetter.kt
// Modified to work with Coil3
private class CoilImageGetter(
    private val textView: TextView,
    private val imageLoader: ImageLoader = ImageLoader(textView.context),
    private val sourceModifier: ((source: String) -> String)? = null
) : Html.ImageGetter {

    override fun getDrawable(source: String): Drawable {
        val finalSource = sourceModifier?.invoke(source) ?: source

        val drawablePlaceholder = DrawablePlaceHolder(textView)
        imageLoader.enqueue(ImageRequest.Builder(textView.context).data(finalSource).apply {
            target { result ->
                val resources = textView.context.resources
                drawablePlaceholder.updateDrawable(result.asDrawable(resources))
                drawablePlaceholder.invalidateSelf()
                // invalidating the drawable doesn't seem to be enough...
                textView.text = textView.text
            }
        }.build())
        // Since this loads async, we return a "blank" drawable, which we update later
        return drawablePlaceholder
    }

    private class DrawablePlaceHolder(
        private val textView: TextView
    ) : BitmapDrawable() {

        private var drawable: Drawable? = null

        override fun draw(canvas: Canvas) {
            drawable?.draw(canvas)
        }

        fun updateDrawable(drawable: Drawable) {
            this.drawable = drawable

            val maxWidth = (textView.measuredWidth - textView.paddingLeft - textView.paddingRight)
                .takeIf { it > 0 } ?: 1

            val intrinsicWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxWidth
            val intrinsicHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxWidth

            val width = maxWidth
            val height = (width.toFloat() * intrinsicHeight / intrinsicWidth).toInt().coerceAtLeast(1)

            drawable.setBounds(0, 0, width, height)
            setBounds(0, 0, width, height)
        }
    }
}
