package org.bu.met810.types.copsandrobbersassets

import org.bu.met810.types.Vectorizable

abstract class Player(val position: (Int, Int)) extends Vectorizable{
  val id: Int = 0
  val moves: List[Move] = List.empty[Move]
  val toVector: Seq[Double] = Seq(position._1.toDouble, position._2.toDouble)
}

case class Robber(override val position: (Int, Int), override val id: Int = 0) extends Player(position){
  override val moves: List[Move] = List(Up, Down, Left, Right)
}

case class Cop(override val position: (Int, Int), override val id: Int = 1) extends Player(position){
  override val moves: List[Move] = List(Up, Down, Left, Right, SkipUp, SkipDown, SkipLeft, SkipRight)
}

