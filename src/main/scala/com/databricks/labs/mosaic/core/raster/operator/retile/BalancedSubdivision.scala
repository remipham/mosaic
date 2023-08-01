package com.databricks.labs.mosaic.core.raster.operator.retile

import com.databricks.labs.mosaic.core.geometry.api.GeometryAPI
import com.databricks.labs.mosaic.core.raster.MosaicRaster
import com.databricks.labs.mosaic.core.raster.api.RasterAPI

import scala.collection.immutable


object BalancedSubdivision {

    def getNumSplits(raster: MosaicRaster, destSize: Int): Int = {
        val size = raster.getMemSize
        val n = size.toDouble / (destSize * 1000 * 1000)
        val nInt = Math.ceil(n).toInt + 1
        Math.pow(4, Math.ceil(Math.log(nInt) / Math.log(4))).toInt
    }

    def getTileSize(x: Int, y: Int, numSplits: Int): (Int, Int) = {
        def split(tile: (Int, Int)): (Int, Int) = {
            val (a, b) = tile
            if (a > b) (a / 2, b) else (a, b / 2)
        }
        var tile = (x, y)
        val originRatio = x.toDouble / y.toDouble
        var i = 0
        while (Math.pow(2, i) < numSplits) {
            i += 1
            tile = split(tile)
        }
        val ratio = tile._1.toDouble / tile._2.toDouble
        // if the ratio is not maintained, split one more time
        // 0.1 is an arbitrary threshold to account for rounding errors
        if (Math.abs(originRatio - ratio) > 0.1) tile = split(tile)
        tile
    }

    def splitRaster(
        mosaicRaster: MosaicRaster,
        sizeInMb: Int,
        geometryAPI: GeometryAPI,
        rasterAPI: RasterAPI
    ): immutable.Seq[MosaicRaster] = {
        val numSplits = getNumSplits(mosaicRaster, sizeInMb)
        val (x, y) = mosaicRaster.getDimensions
        val (tileX, tileY) = getTileSize(x, y, numSplits)
        ReTile.reTile(mosaicRaster, tileX, tileY, geometryAPI, rasterAPI)
    }

}