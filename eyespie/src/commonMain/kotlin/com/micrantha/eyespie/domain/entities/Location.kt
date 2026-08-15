package com.micrantha.eyespie.domain.entities

import com.micrantha.bluebell.platform.Serializable
import com.micrantha.eyespie.domain.logic.distanceTo
import kotlinx.serialization.Serializable as KSerializable

@KSerializable
data class Location(
    val point: Point = Point(),
    val data: Data? = null,
) : Comparable<Location>, Serializable {

    @KSerializable
    data class Point(
        val latitude: Double = Double.NaN,
        val longitude: Double = Double.NaN
    ) : Comparable<Point>, Serializable {

        val isValid = !latitude.isNaN() && !longitude.isNaN()

        override fun toString(): String {
            return "($latitude,$longitude)"
        }

        override fun compareTo(other: Point): Int {
            return distanceTo(latitude, longitude, other.latitude, other.longitude).toInt()
        }
    }

    @KSerializable
    data class Data(
        val name: String? = null,
        val city: String? = null,
        var region: String? = null,
        var country: String? = null,
        var accuracy: Float = Float.NaN
    ) : Comparable<Data>, Serializable {
        override fun compareTo(other: Data) =
            accuracy.compareTo(other.accuracy)
    }

    override fun compareTo(other: Location): Int {
        return point.compareTo(other.point)
    }
}
