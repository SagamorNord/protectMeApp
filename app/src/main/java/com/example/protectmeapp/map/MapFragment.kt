package com.example.protectmeapp.map
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.protectmeapp.databinding.FragmentMapBinding
import com.example.protectmeapp.utils.Constants
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.mapkit.transport.masstransit.SectionMetadata.SectionData
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import java.util.*

class MapFragment: Fragment(), Session.RouteListener {

    companion object {
        private const val ORDER_ID = "ORDER_ID"
        private const val ORDER_DETAILS_SCREEN = -1
        private const val WHOLE_SCHEDULE_COMPLEX_ROUTE_TODAY = 0L
        private const val WHOLE_SCHEDULE_COMPLEX_ROUTE_NEXT_DAY = 1

        fun newInstance(orderId: Long): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    putLong(ORDER_ID, orderId)
                }
            }
        }
    }

    private var _binding: FragmentMapBinding? = null

    private val binding get() = _binding!!

    private val orderId by lazy { arguments?.getLong(ORDER_ID, WHOLE_SCHEDULE_COMPLEX_ROUTE_TODAY) ?: WHOLE_SCHEDULE_COMPLEX_ROUTE_TODAY }

    private var mapObjects: MapObjectCollection? = null

    private var mtRouter: MasstransitRouter? = null

    private var pageIndex: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MapKitFactory.initialize(activity)
        TransportFactory.initialize(activity)
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mtRouter = TransportFactory.getInstance().createMasstransitRouter()
        mapObjects = binding.yaMapview.map.mapObjects.addCollection()
        initViewModel()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.yaMapview.onStart()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initViewModel() {

    }

    private fun showRoute(points: List<Point>) {
        //Очистить маршрут
        mapObjects?.clear()

        val options = MasstransitOptions(
            ArrayList(),
            ArrayList(),
            TimeOptions()
        )

        val requestPoints = mutableListOf<RequestPoint>()

        for (item in points) {
            requestPoints.add(
                RequestPoint(item, RequestPointType.WAYPOINT, null)
            )
        }

        // Установка центральной точки карты
        // Сейчас начальная точка, в дальнейшем высчитывать центр из всех точек
        if (points.isNotEmpty())
            binding.yaMapview.map.move(
                CameraPosition(
                    points[0], 13F, 0F, 0F
                )
            )

        mtRouter?.requestRoutes(requestPoints, options, this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMasstransitRoutes(routes: MutableList<Route>) {
        //Прорисовка альтернативных маршрутов синим
        /*for (route in routes) {
            mapObjects?.addPolyline(route.geometry)
        } */
        if (routes.size > 0) {
            saveRoutes(routes)

            for (section in routes[0].sections) {
                drawSection(
                    section.metadata.data,
                    SubpolylineHelper.subpolyline(
                        routes[0].geometry, section.geometry
                    )
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveRoutes(routes: MutableList<Route>) {
        val routesCache = routes.fold(mutableListOf<ByteArray?>()) { list, route ->
            list.add(mtRouter?.routeSerializer()?.save(route))
            list
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMasstransitRoutesError(error: Error) {
        // Ошибка отрисовки маршрута
    }

    private fun drawSection(
        data: SectionData,
        geometry: Polyline
    ) {
        // Draw a section polyline on a map
        // Set its color depending on the information which the section contains
        val polylineMapObject = mapObjects?.addPolyline(geometry)
        // Masstransit route section defines exactly one on the following
        // 1. Wait until public transport unit arrives
        // 2. Walk
        // 3. Transfer to a nearby stop (typically transfer to a connected
        //    underground station)
        // 4. Ride on a public transport
        // Check the corresponding object for null to get to know which
        // kind of section it is
        if (data.transports != null) {
            // A ride on a public transport section contains information about
            // all known public transport lines which can be used to travel from
            // the start of the section to the end of the section without transfers
            // along a similar geometry
            for (transport in data.transports!!) {
                // Some public transport lines may have a color associated with them
                // Typically this is the case of underground lines
                if (transport.line.style != null) {
                    polylineMapObject?.strokeColor = transport.line.style!!.color!! or -0x1000000
                    return
                }
            }
            // Let us draw bus lines in green and tramway lines in red
            // Draw any other public transport lines in blue
            val knownVehicleTypes: HashSet<String> = HashSet()
            knownVehicleTypes.add("bus")
            knownVehicleTypes.add("tramway")
            for (transport in data.transports!!) {
                val sectionVehicleType = getVehicleType(transport, knownVehicleTypes)
                if (sectionVehicleType == "bus") {
                    polylineMapObject?.strokeColor = -0xff0100 // Green
                    return
                } else if (sectionVehicleType == "tramway") {
                    polylineMapObject?.strokeColor = -0x10000 // Red
                    return
                }
            }
            polylineMapObject?.strokeColor = -0xffff01 // Blue
        } else {
            // This is not a public transport ride section
            // In this example let us draw it in black
            polylineMapObject?.strokeColor = -0x1000000 // Black
        }
    }

    private fun getVehicleType(transport: Transport, knownVehicleTypes: HashSet<String>): String? {
        // A public transport line may have a few 'vehicle types' associated with it
        // These vehicle types are sorted from more specific (say, 'histroic_tram')
        // to more common (say, 'tramway').
        // Your application does not know the list of all vehicle types that occur in the data
        // (because this list is expanding over time), therefore to get the vehicle type of
        // a public line you should iterate from the more specific ones to more common ones
        // until you get a vehicle type which you can process
        // Some examples of vehicle types:
        // "bus", "minibus", "trolleybus", "tramway", "underground", "railway"
        for (type in transport.line.vehicleTypes) {
            if (knownVehicleTypes.contains(type)) {
                return type
            }
        }
        return null
    }

    override fun onStop() {
        binding.yaMapview.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun ArrayList<Point>.add(element: Point?) {
    if (element != null) add(element)

    // Координаты по умолчанию
    else add(Point(Constants.LAT_DEF, Constants.LON_DEF))
}

