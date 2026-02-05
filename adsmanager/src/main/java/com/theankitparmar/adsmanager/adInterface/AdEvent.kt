package com.theankitparmar.adsmanager.adInterface

// 2. Event-driven Communication
//sealed class AdEvent {
//    data class Loaded(val adType: AdType) : AdEvent()
//    data class Failed(val adType: AdType, val error: String) : AdEvent()
//    data class Impression(val adType: AdType) : AdEvent()
//    data class Clicked(val adType: AdType) : AdEvent()
//    data class Revenue(val adType: AdType, val revenue: AdRevenue) : AdEvent()
//    data class Dismissed(val adType: AdType) : AdEvent()
//}