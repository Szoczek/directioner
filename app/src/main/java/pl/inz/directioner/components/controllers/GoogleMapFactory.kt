package pl.inz.directioner.components.controllers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import pl.inz.directioner.R
import pl.inz.directioner.utils.Display

object GoogleMapFactory {

    fun autoZoomLevel(markerList: List<Marker>): CameraUpdate {
        if (markerList.size == 1) {
            val latitude = markerList[0].position.latitude
            val longitude = markerList[0].position.longitude
            val latLng = LatLng(latitude, longitude)

            return CameraUpdateFactory.newLatLngZoom(latLng, 13f)
        } else {
            val builder = LatLngBounds.Builder()
            for (marker in markerList) {
                builder.include(marker.position)
            }
            val bounds = builder.build()

            val padding = 200 // offset from edges of the map in pixels

            return CameraUpdateFactory.newLatLngBounds(bounds, padding)
        }
    }

    fun zoomToUser(userMarker: Marker): CameraUpdate {
        return CameraUpdateFactory.newLatLngZoom(userMarker.position, 20f)
    }

    fun drawMarker(context: Context, text: String, drawable: Drawable? = null): Bitmap {
        var icon = drawable
        if (icon == null)
            icon = context.resources.getDrawable(R.drawable.ic_black_marker, context.theme)

        val bitmap = Bitmap.createBitmap(
            icon!!.intrinsicWidth,
            icon.intrinsicHeight,
            if (icon.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        icon.draw(canvas)
        val paint = Paint()
        paint.textSize = 50 * context.resources.displayMetrics.density / 2
        paint.style = Paint.Style.FILL
        val textCanvas = Canvas(bitmap)
        textCanvas.drawText(
            text,
            ((bitmap.width * 7) / 20).toFloat(),
            (bitmap.height / 2).toFloat(),
            paint
        )

        return bitmap
    }

    fun drawRoute(context: Context): PolylineOptions {
        val polylineOptions = PolylineOptions()
        polylineOptions.width(Display.px2dip(context, 72.toFloat()).toFloat())
        polylineOptions.geodesic(true)
        polylineOptions.color(context.resources.getColor(R.color.colorAccent, context.theme))
        return polylineOptions
    }

    fun drawUserIcon(context: Context): Bitmap {
        val drawable =
            context.resources.getDrawable(R.drawable.ic_baseline_directions_walk_24, context.theme)

        return drawMarker(context, "", drawable)
    }
}