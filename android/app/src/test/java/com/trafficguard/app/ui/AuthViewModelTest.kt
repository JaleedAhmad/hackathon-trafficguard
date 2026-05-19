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
        override suspend fun signInAsGuest(): Result<UserProfile> {
            return Result.Success(UserProfile("anon", null, null, null, isAnonymous = true))
        }
        override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
            return Result.Success(UserProfile("user_123", email, null, "Test"))
        }
        override suspend fun signUpWithEmail(email: String, password: String): Result<UserProfile> {
            return Result.Success(UserProfile("user_123", email, null, "Test"))
        }
        override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
            return Result.Success(UserProfile("google_123", "googleuser@example.com", null, "Google User"))
        }
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
            return Result.Success(Unit)
        }
        override suspend fun getFreshIdToken(): String? = "mock_id_token"
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
        assertEquals("Invalid email address format.", viewModel.uiState.value.emailError)
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
            "Password must be at least 6 characters.",
            viewModel.uiState.value.passwordError
        )
    }
}
