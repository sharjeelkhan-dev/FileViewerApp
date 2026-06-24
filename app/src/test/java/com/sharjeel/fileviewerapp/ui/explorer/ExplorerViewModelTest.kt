package com.sharjeel.fileviewerapp.ui.explorer

import com.sharjeel.fileviewerapp.domain.repository.FileRepository
import android.os.Environment
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerViewModelTest {

    private val repository = mockk<FileRepository>(relaxed = true)
    private lateinit var viewModel: ExplorerViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockkStatic(Environment::class)
        every { Environment.getExternalStorageDirectory() } returns File("/sdcard")
        
        Dispatchers.setMain(testDispatcher)
        viewModel = ExplorerViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Environment::class)
    }

    @Test
    fun `search query update reflects in searchQuery state`() {
        val query = "test_file"
        viewModel.setSearchQuery(query)
        assertEquals(query, viewModel.searchQuery.value)
    }

    @Test
    fun `sorting type update changes the sortType state`() {
        viewModel.setSort(SortType.SIZE, SortOrder.DESCENDING)
        assertEquals(SortType.SIZE, viewModel.sortType.value)
        assertEquals(SortOrder.DESCENDING, viewModel.sortOrder.value)
    }

    @Test
    fun `view mode update changes viewMode state`() {
        viewModel.setViewMode(ViewMode.LARGE)
        assertEquals(ViewMode.LARGE, viewModel.viewMode.value)
    }
}
