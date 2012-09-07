package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import scala.collection.JavaConversions._
import java.io._
import java.util.Arrays

case class RpgMapMetadata(parent: Int,
                          title: String,
                          xSize: Int,
                          ySize: Int,
                          tilesets: List[String]) {
}

case class RpgMap(proj: Project, id: Int, metadata: RpgMapMetadata)
extends Resource[RpgMap, RpgMapMetadata]
{
  def meta = RpgMap
  def name = RpgMap.idToName(id)
  
  def saveMapData(d: RpgMapData) =
    d.writeToFile(proj, name)
  
  def readMapData() : Option[RpgMapData] = 
    RpgMapData.readFromDisk(proj, name)  
}

/*
 * An explanation of the data format.
 * 
 * Each tile on the map is comprised of 3 bytes.
 * 
 * Byte 1 value:
 * -2 = autotile
 * -1 = empty tile
 * 0-127 = one of the 128 tilesets possible
 * 
 * Byte 2 value:
 * If autotile, then the autotile number from 0-255
 * If regular tile, then x tile index ranging from 0-255
 * If empty, ignored.
 * 
 * Byte 3 value:
 * If autotile, then this byte describes the border configuration.
 *    See Autotile.DirectionMasks for how this works specifically.
 * If regular tile, then the y tile index from 0-255
 * If empty, ignored
 */
object RpgMap extends MetaResource[RpgMap, RpgMapMetadata] {
  def rcType = "map"
  def keyExts = Array(metadataExt)
  
  def idToName(id: Int) = "Map%d".format(id)
  
  def apply(proj: Project, name: String, metadata: RpgMapMetadata) = 
    apply(proj, name.drop(3).toInt, metadata)
  
  def readFromDisk(proj: Project, id: Int) : RpgMap = 
    readFromDisk(proj, idToName(id))
    
  override def rcDir(proj: Project) = proj.mapsDir

  val initXSize = 20
  val initYSize = 15
  
  val bytesPerTile = 3
  
  val autotileByte : Byte = -2
  val emptyTileByte : Byte = -1
    
  def defaultInstance(proj: Project, name: String) : RpgMap = {
    val m = RpgMapMetadata(-1, "Starting Map",
                           initXSize, initYSize, 
                           List("Refmap-TileA5",
                                "Refmap-TileB",
                                "Refmap-TileC",
                                "Refmap-TileD",
                                "Refmap-TileE"))
    RpgMap(proj, name, m)
  }
  def defaultInstance(proj: Project, id: Int) : RpgMap = 
    defaultInstance(proj, idToName(id))
  
  def emptyMapData(xSize: Int, ySize: Int) = {
    val autoLayer  = {
      // Generate a 3-byte tile
      val a = Array[Byte](autotileByte,0,0)
      // Make a whole row of that
      val row = Array.tabulate[Byte](xSize*bytesPerTile)(i => a(i%a.length))
      // Make multiple rows
      Array.tabulate[Array[Byte]](ySize)(i => row.clone())
    }
    val emptyLayer = { 
      val a = Array[Byte](emptyTileByte,0,0)
      val row = Array.tabulate[Byte](xSize*bytesPerTile)(i => a(i%a.length))
      Array.tabulate[Array[Byte]](ySize)(i => row.clone())
    }
    
    RpgMapData(autoLayer, emptyLayer, emptyLayer, Array.empty)
  }
  
  def defaultMapData = emptyMapData(initXSize, initYSize)
  
}
