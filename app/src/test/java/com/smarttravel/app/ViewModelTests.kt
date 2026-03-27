package com.smarttravel.app

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smarttravel.app.data.remote.model.NominatimAddress
import com.smarttravel.app.data.remote.model.NominatimPlace
import com.smarttravel.app.repository.Result
import com.smarttravel.app.repository.SearchRepository
import com.smarttravel.app.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class SearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var searchRepository: SearchRepository

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(searchRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search with short query does not trigger API call`() = runTest {
        viewModel.search("ab") // less than 3 chars
        advanceUntilIdle()
        verify(searchRepository, never()).searchPlaces(any())
    }

    @Test
    fun `search with valid query updates results`() = runTest {
        val mockPlaces = listOf(
            NominatimPlace(
                placeId = 1L,
                displayName = "Mumbai, Maharashtra, India",
                lat = "19.0760",
                lon = "72.8777",
                type = "city",
                address = NominatimAddress("Marine Drive", "Mumbai", "Maharashtra", "India", "400001")
            )
        )
        `when`(searchRepository.searchPlaces("Mumbai")).thenReturn(Result.Success(mockPlaces))

        viewModel.search("Mumbai")
        advanceTimeBy(500) // past debounce
        advanceUntilIdle()

        val result = viewModel.searchResults.value
        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.size)
        assertEquals("Mumbai, Maharashtra, India", result.data[0].displayName)
    }

    @Test
    fun `search failure sets error result`() = runTest {
        `when`(searchRepository.searchPlaces("xyz")).thenReturn(Result.Error("Network error"))

        viewModel.search("xyz")
        advanceTimeBy(500)
        advanceUntilIdle()

        val result = viewModel.searchResults.value
        assertTrue(result is Result.Error)
        assertEquals("Network error", (result as Result.Error).message)
    }

    @Test
    fun `select place updates selectedPlace LiveData`() {
        val place = NominatimPlace(
            placeId = 42L,
            displayName = "Delhi, India",
            lat = "28.6139",
            lon = "77.2090",
            type = "city",
            address = null
        )
        viewModel.selectPlace(place)
        assertEquals(place, viewModel.selectedPlace.value)
    }

    @Test
    fun `clearSelection sets selectedPlace to null`() {
        val place = NominatimPlace(1L, "Test", "0.0", "0.0", "city", null)
        viewModel.selectPlace(place)
        viewModel.clearSelection()
        assertNull(viewModel.selectedPlace.value)
    }
}
