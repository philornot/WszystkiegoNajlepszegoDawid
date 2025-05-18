package com.philornot.siekiera

import com.philornot.siekiera.config.AppConfigTest
import com.philornot.siekiera.network.DriveApiClientTest
import com.philornot.siekiera.notification.NotificationSchedulerTest
import com.philornot.siekiera.ui.viewmodel.MainViewModelTest
import com.philornot.siekiera.utils.TimeProviderTest
import com.philornot.siekiera.utils.TimeSimulatorTest
import com.philornot.siekiera.utils.TimeUtilsTest
import com.philornot.siekiera.workers.FileCheckWorkerTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite that runs all unit tests for the application. Use this to run
 * all tests at once and get comprehensive test coverage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Time-related tests
    TimeUtilsTest::class,
    TimeProviderTest::class,
    TimeSimulatorTest::class,

    // Config and ViewModel tests
    AppConfigTest::class,
    MainViewModelTest::class,

    // Service and component tests
    NotificationSchedulerTest::class,
    DriveApiClientTest::class,
    FileCheckWorkerTest::class,
)
class AllUnitTestsSuite {
    // This class only serves as a holder for the suite annotations
}