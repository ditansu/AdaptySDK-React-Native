package com.rnadapty.react

import com.adapty.Adapty
import com.adapty.api.AttributionType
import com.adapty.utils.LogLevel
import com.facebook.react.bridge.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AdaptyModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {
    val gson = Gson()

    override fun getName(): String {
        return "RNAdapty"
    }

    @ReactMethod
    fun activate(sdkKey: String, customerUserId: String, observerMode: Boolean, logLevel: String, promise: Promise) {
        Adapty.activate(reactApplicationContext, sdkKey, customerUserId)

        when (logLevel) {
            "verbose" -> Adapty.setLogLevel(LogLevel.VERBOSE)
            "error" -> Adapty.setLogLevel(LogLevel.ERROR)
            else -> Adapty.setLogLevel(LogLevel.NONE)
        }

        if (observerMode) {
            Adapty.syncPurchases { }
        }

        promise.resolve(true)
    }

    @ReactMethod
    fun logout(promise: Promise) {
        Adapty.logout { error ->
            if (error != null) {
                promise.reject("Error in: logout", error)
                return@logout;
            }
            promise.resolve(true)
            return@logout;
        }
    }

    @ReactMethod
    fun identify(customerUserId: String, promise: Promise) {
        Adapty.identify(customerUserId) {error ->
            if (error != null) {
                promise.reject("Error in: identify", error)
                return@identify
            }
            promise.resolve(true)
            return@identify
        }
    }

    @ReactMethod // @todo
    fun updateProfile(promise: Promise) {
//        val params = ProfileParameterBuilder()

//        Adapty.updateProfile { error ->
//            if (error != null) {
//                promise.reject("","")
//                return@updateProfile
//            }
//
//            promise.resolve(12)
//        }
    }

    @ReactMethod // @todo check
    fun getPaywalls(promise: Promise) {
        Adapty.getPaywalls { paywalls, products, state, error ->
            if (error != null) {
                promise.reject("Error in: getPaywalls",error)
                return@getPaywalls
            }

            val hm: HashMap<String, Any> = HashMap()
            hm["products"] = gson.toJson(products)
            hm["paywalls"] = gson.toJson(paywalls)

            val map = convertMapToWriteableMap(hm)
            promise.resolve(map)
        }
    }

    @ReactMethod // @todo
    fun makePurchase(productId: String, promise: Promise) {
//        Adapty.makePurchase("", "23") {
//
//        }
        promise.resolve(true)
    }

    @ReactMethod
    fun restorePurchases(promise: Promise) {
        Adapty.restorePurchases { _, error ->
            if (error != null) {
                promise.reject("Error in: restorePurchases", error)
                return@restorePurchases
            }

            promise.resolve(true)
        }
    }

    @ReactMethod // @todo
    fun validateReceipt(receipt: String, promise: Promise) {
        Adapty.validatePurchase("","", receipt) {  _, error ->

        }
    }

    @ReactMethod
    fun getPromo(promise: Promise) {
        Adapty.getPromo { promo, error ->
            if (error != null) {
                promise.reject("Error in: getPromo", error)
                return@getPromo
            }

            val promoMap = promo.serializeToMap()
            promise.resolve(promoMap)
        }
    }

    @ReactMethod
    fun getPurchaseInfo(promise: Promise) {
        Adapty.getPurchaserInfo { purchaserInfo, _, error ->
            if (error != null) {
                promise.reject("Error in: getPurchaserInfo", error)
                return@getPurchaserInfo
            }

            val json = gson.toJson(purchaserInfo)
            promise.resolve(json)
        }
    }

    @ReactMethod
    fun updateAttribution(map: ReadableMap, source: String, promise: Promise) {
        var sourceType = ""

        when (source) {
            "Branch" -> sourceType = AttributionType.BRANCH
            "Adjust" -> sourceType = AttributionType.ADJUST
            "AppsFlyer" -> sourceType = AttributionType.APPSFLYER
            else -> {
                promise.reject("Error in: updateAttribution","Source of attribution is not defined")
                return@updateAttribution
            }
        }

        Adapty.updateAttribution(map, sourceType)
        promise.resolve(true)
    }

    private fun convertMapToWriteableMap(map: Map<String, *>): WritableMap {
        val writableMap: WritableMap = WritableNativeMap()
        for ((key, value) in map) {
            when (value) {
                null -> writableMap.putNull(key)
                is Boolean -> writableMap.putBoolean(key, value)
                is Int -> writableMap.putInt(key, value)
                is Double -> writableMap.putDouble(key, value)
                is String -> writableMap.putString(key, value)
                is Map<*, *> -> writableMap.putMap(key, convertMapToWriteableMap(value as Map<String, *>))
                is Array<*> -> writableMap.putArray(key, convertArrayToWritableArray(value as Array<Any?>))
                is List<*> -> writableMap.putArray(key, convertArrayToWritableArray(value.toTypedArray()))
            }
        }
        return writableMap
    }

    //convert an object of type I to type O
    inline fun <I, reified O> I.convert(): O {
        val json = gson.toJson(this)
        return gson.fromJson(json, object : TypeToken<O>() {}.type)
    }

    //convert a data class to a map
    fun <T> T.serializeToMap(): Map<String, Any> {
        return convert()
    }



    private fun convertArrayToWritableArray(array: Array<Any?>): WritableArray {
        val writableArray: WritableArray = WritableNativeArray()
        for (item in array) {
            when (item) {
                null -> writableArray.pushNull()
                is Boolean -> writableArray.pushBoolean(item)
                is Int -> writableArray.pushInt(item)
                is Double -> writableArray.pushDouble(item)
                is String -> writableArray.pushString(item)
                is Map<*, *> -> writableArray.pushMap(convertMapToWriteableMap(item as Map<String, *>))
                is Array<*> -> writableArray.pushArray(convertArrayToWritableArray(item as Array<Any?>))
                is List<*> -> writableArray.pushArray(convertArrayToWritableArray(item.toTypedArray()))
            }
        }
        return writableArray
    }
}
