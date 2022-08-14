package com.wasteleaf.wasteleaf_android.home.maps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.annotation.Nullable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.wasteleaf.wasteleaf_android.util.Constants.getReportFromArray
import com.wasteleaf.wasteleaf_android.R


class CustomInfoWindowClass(private val mContext: Context) : GoogleMap.InfoWindowAdapter {
    private val mWindow: View = LayoutInflater.from(mContext).inflate(R.layout.info_window, null)

    private fun renderWindowImage(marker: Marker, view: View) {
        if (marker.title == "yourMarker") {
            (view.findViewById<View>(R.id.imageView4) as ImageView).visibility = INVISIBLE
            (view.findViewById<View>(R.id.imageViewMarker) as ImageView).visibility = INVISIBLE
            return
        }
        (view.findViewById<View>(R.id.imageView4) as ImageView).visibility = VISIBLE
        (view.findViewById<View>(R.id.imageViewMarker) as ImageView).visibility = VISIBLE

        val reportData = getReportFromArray(MapsActivity.reportListObj, marker)?.get("data") as HashMap<*, *>
        val imageViewMarker = view.findViewById<View>(R.id.imageViewMarker) as ImageView

        val image = images[marker]
        if (image == null) {
            Glide.with(mContext).asBitmap().load((reportData["photoRef"] as ArrayList<*>).last()).into(getTarget(marker))
        } else {
            imageViewMarker.setImageBitmap(image)
        }
    }

    override fun getInfoWindow(marker: Marker): View {
        renderWindowImage(marker, mWindow)
        return mWindow
    }

    override fun getInfoContents(marker: Marker): View {
        renderWindowImage(marker, mWindow)
        return mWindow
    }

    private val images: HashMap<Marker, Bitmap> = HashMap()
    private val targets: HashMap<Marker, CustomTarget<Bitmap>> = HashMap()

    inner class InfoTarget(var marker: Marker) : CustomTarget<Bitmap>() {
        override fun onLoadCleared(@Nullable placeholder: Drawable?) {
            images.remove(marker)
        }

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            images[marker] = resource
            marker.showInfoWindow()
        }
    }

    private fun getTarget(marker: Marker): CustomTarget<Bitmap> {
        var target = targets[marker]
        if (target == null) {
            target = InfoTarget(marker)
            targets[marker] = target // missing in original (fixed 2018 August)
        }
        return target
    }
}
