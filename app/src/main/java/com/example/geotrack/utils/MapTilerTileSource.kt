package com.example.geotrack.utils

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/**
 * High-performance Dark Mode tile source for Osmdroid.
 * Uses CartoDB Dark Matter (ultra-fast, free, no API key limit) to match the dark neon AMOLED theme,
 * and supports MapTiler Cloud styles when an API key is present.
 */
object MapTilerTileSource {

    /**
     * CartoDB Dark Matter tile source - 100% reliable dark mode map tiles without API key errors.
     */
    val CARTO_DARK: OnlineTileSourceBase = object : XYTileSource(
        "CartoDark",
        0,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/dark_all/",
            "https://b.basemaps.cartocdn.com/dark_all/",
            "https://c.basemaps.cartocdn.com/dark_all/",
            "https://d.basemaps.cartocdn.com/dark_all/"
        ),
        "© OpenStreetMap, © CARTO"
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return getBaseUrl() + "$zoom/$x/$y.png"
        }
    }

    /**
     * MapTiler Dark Tile Source
     */
    fun createDarkTileSource(apiKey: String = Constants.MAPTILER_API_KEY): OnlineTileSourceBase {
        // If placeholder or empty, use CartoDB Dark Matter for guaranteed instant loading
        if (apiKey.isBlank() || apiKey.startsWith("get_your_own")) {
            return CARTO_DARK
        }
        return object : XYTileSource(
            "MapTilerDark",
            0,
            20,
            256,
            ".png",
            arrayOf("https://api.maptiler.com/maps/streets-v2-dark/256/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return getBaseUrl() + "$zoom/$x/$y.png?key=$apiKey"
            }
        }
    }

    /**
     * MapTiler Streets Tile Source
     */
    fun createStreetsTileSource(apiKey: String = Constants.MAPTILER_API_KEY): OnlineTileSourceBase {
        if (apiKey.isBlank() || apiKey.startsWith("get_your_own")) {
            return CARTO_DARK
        }
        return object : XYTileSource(
            "MapTilerStreets",
            0,
            20,
            256,
            ".png",
            arrayOf("https://api.maptiler.com/maps/${Constants.MAPTILER_STREETS_STYLE_ID}/256/")
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return getBaseUrl() + "$zoom/$x/$y.png?key=$apiKey"
            }
        }
    }
}
