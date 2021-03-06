package org.bu.simmba.types.copsandrobbersassets

import org.bu.simmba.types.Vectorizable

case class Building(position: (Int, Int), length: Int, width: Int) extends Vectorizable {
  val toVector: Seq[Double] = Seq(position._1, position._2, length, width).map(_.toDouble)
}
