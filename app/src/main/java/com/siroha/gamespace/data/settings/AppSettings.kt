package com.siroha.gamespace.data.settings

enum class DarkModePreference { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    /** Only meaningful when the resolved theme is actually dark. */
    val amoledEnabled: Boolean = false,
    val dynamicColorEnabled: Boolean = true,
    val homeGridView: Boolean = true,
    /** Whether System Access has been shown once already — lets Home
     *  become the real start destination on subsequent launches instead
     *  of the onboarding gate every time. */
    val onboardingCompleted: Boolean = false
)
