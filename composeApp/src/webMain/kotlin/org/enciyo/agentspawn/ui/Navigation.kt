package org.enciyo.agentspawn.ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("home")
data object StartScreen

@Serializable
@SerialName("onboarding")
data object OnboardingScreen

@Serializable
@SerialName("chat")
data object ChatScreen
