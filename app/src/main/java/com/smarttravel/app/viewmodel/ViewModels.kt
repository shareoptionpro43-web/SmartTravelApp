package com.smarttravel.app.viewmodel

import android.location.Location
import androidx.lifecycle.*
import com.smarttravel.app.data.local.entity.SavedPlace
import com.smarttravel.app.data.remote.model.NominatimPlace
import com.smarttravel.app.data.remote.model.NearbyPlace
import com.smarttravel.app.data.remote.model.PlaceCategory
import com.smarttravel.app.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

// ── Map ViewModel ──────────────────────────────────────────────────────────
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModel() {

    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation

    private val _routeResult = MutableLiveData<Result<RouteRepository.RouteResult>>()
    val routeResult: LiveData<Result<RouteRepository.RouteResult>> = _routeResult

    private val _fuelCost = MutableLiveData<Double>()
    val fuelCost: LiveData<Double> = _fuelCost

    val savedPlaces = savedPlaceRepository.getAllPlaces().asLiveData()

    private var activeSessionId: Long? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun onLocationUpdated(location: Location) {
        _currentLocation.value = location
        viewModelScope.launch {
            activeSessionId?.let { id ->
                locationRepository.addLocationPoint(
                    sessionId = id,
                    lat = location.latitude,
                    lng = location.longitude,
                    accuracy = location.accuracy,
                    speed = location.speed
                )
            }
        }
    }

    fun startTracking(location: Location) {
        viewModelScope.launch {
            val date = dateFormat.format(Date())
            val id = locationRepository.startSession(location.latitude, location.longitude, date)
            activeSessionId = id
        }
    }

    fun stopTracking(location: Location) {
        viewModelScope.launch {
            activeSessionId?.let { id ->
                locationRepository.endSession(id, location.latitude, location.longitude)
                activeSessionId = null
            }
        }
    }

    fun calculateRoute(
        srcLat: Double, srcLon: Double,
        dstLat: Double, dstLon: Double
    ) {
        viewModelScope.launch {
            _routeResult.value = Result.Loading
            _routeResult.value = routeRepository.getRoute(srcLat, srcLon, dstLat, dstLon)
        }
    }

    fun calculateFuelCost(distanceKm: Double, fuelPrice: Double, mileage: Double) {
        if (mileage > 0) {
            _fuelCost.value = (distanceKm / mileage) * fuelPrice
        }
    }

    fun savePlace(name: String, label: String, address: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            savedPlaceRepository.savePlace(
                SavedPlace(name = name, label = label, address = address, latitude = lat, longitude = lng)
            )
        }
    }
}

// ── Search ViewModel ───────────────────────────────────────────────────────
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<Result<List<NominatimPlace>>>()
    val searchResults: LiveData<Result<List<NominatimPlace>>> = _searchResults

    private val _selectedPlace = MutableLiveData<NominatimPlace?>()
    val selectedPlace: LiveData<NominatimPlace?> = _selectedPlace

    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        if (query.length < 3) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(400) // debounce
            _searchResults.value = Result.Loading
            _searchResults.value = searchRepository.searchPlaces(query)
        }
    }

    fun selectPlace(place: NominatimPlace) {
        _selectedPlace.value = place
    }

    fun clearSelection() {
        _selectedPlace.value = null
    }
}

// ── Nearby ViewModel ───────────────────────────────────────────────────────
@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val nearbyRepository: NearbyRepository
) : ViewModel() {

    private val _places = MutableLiveData<Result<List<NearbyPlace>>>()
    val places: LiveData<Result<List<NearbyPlace>>> = _places

    private val _selectedCategory = MutableLiveData(PlaceCategory.HOTEL)
    val selectedCategory: LiveData<PlaceCategory> = _selectedCategory

    fun loadNearby(lat: Double, lon: Double, category: PlaceCategory) {
        _selectedCategory.value = category
        viewModelScope.launch {
            _places.value = Result.Loading
            _places.value = nearbyRepository.getNearbyPlaces(lat, lon, category)
        }
    }
}

// ── Analytics ViewModel ────────────────────────────────────────────────────
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val exportHelper: com.smarttravel.app.utils.CsvExportHelper
) : ViewModel() {

    val recentSummaries = analyticsRepository.getRecentSummaries(30).asLiveData()
    val allSessions = analyticsRepository.getAllSessions().asLiveData()

    private val _exportResult = MutableLiveData<Result<String>>()
    val exportResult: LiveData<Result<String>> = _exportResult

    fun exportToCSV() {
        viewModelScope.launch {
            _exportResult.value = Result.Loading
            try {
                val sessions = analyticsRepository.getAllSessionsOnce()
                val path = exportHelper.export(sessions)
                _exportResult.value = Result.Success(path)
            } catch (e: Exception) {
                _exportResult.value = Result.Error(e.message ?: "Export failed", e)
            }
        }
    }
}

// ── Saved Places ViewModel ─────────────────────────────────────────────────
@HiltViewModel
class SavedPlacesViewModel @Inject constructor(
    private val repository: SavedPlaceRepository
) : ViewModel() {

    val places = repository.getAllPlaces().asLiveData()

    fun deletePlace(place: SavedPlace) = viewModelScope.launch {
        repository.deletePlace(place)
    }

    fun updatePlace(place: SavedPlace) = viewModelScope.launch {
        repository.updatePlace(place)
    }
}
