package pl.inz.directioner.components.controllers

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import pl.inz.directioner.api.models.Route

class GoogleMapController(context: Context, googleMap: GoogleMap) {
    private val mContext: Context = context
    private val mGoogleMap: GoogleMap = googleMap

    private val mTimeSquare = LatLng(40.758895, -73.985131)

    private var mSpotMarkerList = ArrayList<Marker>()

    private var mRouteMarkerList = ArrayList<Marker>()
    private lateinit var mRoutePolyline: Polyline
    private var mUserMarker: Marker? = null

    fun animateZoomInCamera() {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mTimeSquare, 15f))
    }

    fun animateZoomOutCamera() {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mTimeSquare, 10f))
    }

    fun clearMarkers() {
        for (marker in mSpotMarkerList) {
            marker.remove()
        }
        mSpotMarkerList.clear()
    }

    fun setMarkersAndRoute(route: Route) {
        val polylineOptions = GoogleMapFactory.drawRoute(mContext)

        route.legs.forEach { leg ->
            leg.steps.forEach {
                val startLatLng =
                    LatLng(it.startLocation.lat, it.startLocation.lng)
                val startMarkerOptions = MarkerOptions().position(startLatLng)
                    .icon(
                        BitmapDescriptorFactory
                            .fromBitmap(
                                GoogleMapFactory.drawMarker(
                                    mContext,
                                    "${leg.steps.indexOf(it)}"
                                )
                            )
                    )

                val endLatLng =
                    LatLng(it.endLocation.lat, it.endLocation.lng)
                val endMarkerOptions = MarkerOptions().position(endLatLng)
                    .icon(
                        BitmapDescriptorFactory
                            .fromBitmap(
                                GoogleMapFactory.drawMarker(
                                    mContext,
                                    "${leg.steps.indexOf(it)}"
                                )
                            )
                    )

                val startMarker = mGoogleMap.addMarker(startMarkerOptions)
                val endMarker = mGoogleMap.addMarker(endMarkerOptions)
                mRouteMarkerList.add(startMarker)
                mRouteMarkerList.add(endMarker)

                val pointsList = PolyUtil.decode(it.polyline.points)
                for (point in pointsList) {
                    polylineOptions.add(point)
                }
            }
        }
        mRoutePolyline = mGoogleMap.addPolyline(polylineOptions)

        val currentLocation = LatLng(
            route.legs.first().startLocation.lat,
            route.legs.first().startLocation.lng
        )
//        replaceUserMarker(currentLocation)
    }

    fun replaceUserMarker(currentLocation: LatLng) {
        if (mUserMarker != null)
            mUserMarker!!.remove()

        val currentMarkerOptions = MarkerOptions().position(currentLocation)
            .icon(
                BitmapDescriptorFactory
                    .fromBitmap(
                        GoogleMapFactory.drawUserIcon(
                            mContext
                        )
                    )
            )

        mUserMarker = mGoogleMap.addMarker(currentMarkerOptions)

        mGoogleMap.animateCamera(GoogleMapFactory.zoomToUser(mUserMarker!!))
    }

    fun clearMarkersAndRoute() {
        for (marker in mRouteMarkerList) {
            marker.remove()
        }
        mRouteMarkerList.clear()

        if (::mRoutePolyline.isInitialized) {
            mRoutePolyline.remove()
        }
    }
}