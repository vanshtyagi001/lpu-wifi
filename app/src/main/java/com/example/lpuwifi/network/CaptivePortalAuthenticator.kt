package com.example.lpuwifi.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class CaptivePortalAuthenticator {

    private val TAG = "CaptivePortalAuth"
    private val LOGIN_URL = "https://internet.lpu.in/24online/webpages/client.jsp"

    // We need a CookieJar to remember the session between the GET (loading page) and POST (submitting form)
    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies.toMutableList()
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: mutableListOf()
        }
    }

    // Configure OkHttpClient with timeouts and Cookie handling
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Automates the Captive Portal Login Process
     */
    suspend fun automateLogin(regNo: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting captive portal login process...")

            // Ensure registration number has the required @lpu.com domain
            val username = if (regNo.contains("@lpu.com")) regNo else "$regNo@lpu.com"

            // STEP 1: Fetch the login page to get the session cookies and hidden form fields
            val getRequest = Request.Builder()
                .url(LOGIN_URL)
                .get()
                .build()

            val getResponse = client.newCall(getRequest).execute()
            val html = getResponse.body?.string()

            if (!getResponse.isSuccessful || html == null) {
                Log.e(TAG, "Failed to load login page. Code: ${getResponse.code}")
                return@withContext false
            }

            Log.d(TAG, "Login page loaded successfully. Parsing form...")

            // STEP 2: Parse the HTML using Jsoup to find the login form
            val document = Jsoup.parse(html)

            // Try to find form by name (like in your JS) or default to the first form
            val form = document.select("form[name=clientloginform]").first()
                ?: document.select("form").first()

            if (form == null) {
                Log.e(TAG, "Login form not found on the page!")
                return@withContext false
            }

            // Determine where the form is supposed to be submitted to
            var actionUrl = form.attr("action")
            if (actionUrl.isBlank()) {
                actionUrl = LOGIN_URL // Default back to the same page if action is empty
            } else if (!actionUrl.startsWith("http")) {
                // Handle relative URLs
                actionUrl = "https://internet.lpu.in/24online/webpages/$actionUrl"
            }

            // STEP 3: Extract all hidden inputs. Captive portals often use dynamic tokens (e.g., magic numbers)
            val formBuilder = FormBody.Builder()
            val inputs = form.select("input")

            for (input in inputs) {
                val type = input.attr("type")
                val name = input.attr("name")
                val value = input.attr("value")

                // Add hidden fields automatically
                if (type.equals("hidden", ignoreCase = true) && name.isNotBlank()) {
                    formBuilder.add(name, value)
                }
            }

            // STEP 4: Add the user credentials and the required policy agreement
            formBuilder.add("username", username)
            formBuilder.add("password", pass)

            // Your JS checks '#agreepolicy'. Server forms usually expect 'on' or 'true' for checkboxes
            formBuilder.add("agreepolicy", "on")

            val formBody = formBuilder.build()

            // STEP 5: Submit the form via POST request
            val postRequest = Request.Builder()
                .url(actionUrl)
                .post(formBody)
                .header("Referer", LOGIN_URL) // Some servers require the referer header for security
                .build()

            Log.d(TAG, "Submitting login credentials to: $actionUrl")
            val postResponse = client.newCall(postRequest).execute()
            val responseBody = postResponse.body?.string() ?: ""

            // STEP 6: Verify if login was successful
            // Usually, 24online redirects to 'E24onlineHTTPClient' or shows a success message
            val isSuccess = responseBody.contains("successfully logged in", ignoreCase = true) ||
                    responseBody.contains("E24onlineHTTPClient", ignoreCase = true) ||
                    postResponse.isRedirect

            if (isSuccess) {
                Log.d(TAG, "Authentication SUCCESSFUL!")
                return@withContext true
            } else {
                Log.e(TAG, "Authentication FAILED. Response did not contain success indicators.")
                // You can log responseBody here if debugging is needed later
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during login automation: ${e.localizedMessage}")
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Checks if the device already has active internet access
     * (meaning we are already logged in or using mobile data).
     */
    suspend fun hasInternetAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Google's specific 204 (No Content) endpoint.
            // If it returns 204, internet is working. If it redirects, we are stuck in a Captive Portal.
            val request = Request.Builder()
                .url("http://clients3.google.com/generate_204")
                .build()

            val response = client.newCall(request).execute()
            val success = response.code == 204
            response.close()
            return@withContext success
        } catch (e: Exception) {
            return@withContext false
        }
    }
}