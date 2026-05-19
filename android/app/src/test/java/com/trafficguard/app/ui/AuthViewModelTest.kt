package com.traffic_guard.ai.ui

import com.traffic_guard.ai.data.AuthRepository
import com.traffic_guard.ai.data.Result
import com.traffic_guard.ai.data.UserProfile
import com.traffic_guard.ai.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockRepository = object : AuthRepository {
        override val currentUser: Flow<UserProfile?> = emptyFlow()
        override suspend fun signInAnonymously(): Result<UserProfile> {
            return Result.Success(UserProfile("anon", null, null, null, true))
        }
        override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
            return Result.Success(UserProfile("user_123", email, null, "Test", false))
        }
        override suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile> {
            return Result.Success(UserProfile("user_123", email, null, "Test", false))
        }
        override suspend fun sendOtpCode(phoneNumber: String): Result<String> {
            return Result.Success("verification_id")
        }
        override suspend fun verifyOtpCode(verificationId: String, code: String): Result<UserProfile> {
            return Result.Success(UserProfile("phone_123", null, "+1234567890", "Mobile", false))
        }
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
            return Result.Success(Unit)
        }
        override suspend fun signOut() {}
    }

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testValidEmailFormat_setsErrorToNull() {
        viewModel.onEmailChanged("test@example.com")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun testInvalidEmailFormat_setsCorrectRomanUrduErrorMessage() {
        viewModel.onEmailChanged("invalid-email")
        assertEquals("Ghalat email format (Invalid Email)", viewModel.uiState.value.emailError)
    }

    @Test
    fun testValidPasswordLength_setsErrorToNull() {
        viewModel.onPasswordChanged("securepassword123")
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun testTooShortPasswordLength_setsCorrectRomanUrduErrorMessage() {
        viewModel.onPasswordChanged("123")
        assertEquals(
            "Password kam az kam 8 characters hona chahiye (Min 8 chars)",
            viewModel.uiState.value.passwordError
        )
    }

    @Test
    fun testValidPhoneDigits_setsErrorToNull() {
        viewModel.onPhoneChanged("03001234567")
        assertNull(viewModel.uiState.value.phoneError)
    }

    @Test
    fun testInvalidPhoneDigits_setsCorrectRomanUrduErrorMessage() {
        viewModel.onPhoneChanged("invalid-phone-digits-letters")
        assertEquals("Sirf numbers darj karein (Numbers only)", viewModel.uiState.value.phoneError)
    }
}
