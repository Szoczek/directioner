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

//    fun setCustomMarker() {
////        val blackMarkerIcon: BitmapDescriptor =
////            BitmapDescriptorFactory.fromResource(R.drawable.ic_spot_marker)
////        val markerOptions: MarkerOptions =
////            MarkerOptions().position(mTimeSquare).title(mContext.getString(R.string.time_square))
////                .snippet(mContext.getString(R.string.i_am_snippet)).icon(blackMarkerIcon)
////        mGoogleMap.addMarker(markerOptions)
////        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(mTimeSquare))
////    }

    fun animateZoomInCamera() {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mTimeSquare, 15f))
    }

    fun animateZoomOutCamera() {
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mTimeSquare, 10f))
    }

//    fun setMarkersAndZoom(spotList: List<Spot>) {
//        val spotBitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_spot_marker)
//
//        for (spot in spotList) {
//            val name = spot.name
//            val latitude = spot.lat
//            val longitude = spot.lng
//            val latLng = LatLng(latitude!!, longitude!!)
//            val markerOptions = MarkerOptions()
//            markerOptions.position(latLng).title(name).icon(spotBitmap)
//
//            val marker = mGoogleMap.addMarker(markerOptions)
//            mSpotMarkerList.add(marker)
//        }
//
//        mGoogleMap.animateCamera(GoogleMapFactory.autoZoomLevel(mSpotMarkerList))
//    }

    fun clearMarkers() {
        for (marker in mSpotMarkerList) {
            marker.remove()
        }
        mSpotMarkerList.clear()
    }

    fun setMarkersAndRoute(route: Route) {
        val startLatLng =
            LatLng(route.legs.first().startLocation.lat, route.legs.first().startLocation.lng)
        val startMarkerOptions: MarkerOptions = MarkerOptions().position(startLatLng)
            .title(route.legs.first().startAddress)
            .icon(
                BitmapDescriptorFactory
                    .fromBitmap(GoogleMapFactory.drawMarker(mContext, "S"))
            )

        val endLatLng = LatLng(route.legs.last().endLocation.lat, route.legs.last().endLocation.lng)
        val endMarkerOptions: MarkerOptions = MarkerOptions().position(endLatLng)
            .title(route.legs.last().endAddress)
            .icon(
                BitmapDescriptorFactory
                    .fromBitmap(GoogleMapFactory.drawMarker(mContext, "E"))
            )

        val startMarker = mGoogleMap.addMarker(startMarkerOptions)
        val endMarker = mGoogleMap.addMarker(endMarkerOptions)
        mRouteMarkerList.add(startMarker)
        mRouteMarkerList.add(endMarker)

        val polylineOptions = GoogleMapFactory.drawRoute(mContext)
        val pointsList = PolyUtil.decode(route.overview_polyline.points)
        for (point in pointsList) {
            polylineOptions.add(point)
        }

        mRoutePolyline = mGoogleMap.addPolyline(polylineOptions)

        mGoogleMap.animateCamera(GoogleMapFactory.autoZoomLevel(mRouteMarkerList))
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