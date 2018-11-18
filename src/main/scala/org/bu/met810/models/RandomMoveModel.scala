package org.bu.met810.models

import org.bu.met810.types.boardassets.{Board, Player}
import org.bu.met810.types.moves._
import org.bu.met810.choose

/**
  * Selects a move at uniform random from a set of possible moves
  */
class RandomMoveModel extends PlayerModel[Board, Player, Move] {
  override def selectMove(playerId: Int, board: Board): Move = {
    val player = Set(board.p1, board.p2).find(_.id == playerId) match {
      case Some(p) => p
      case None =>  throw new NoSuchElementException(s"unable to find player with id $playerId!")
    }
    val (x,y) = player.position
    val validMoves: List[Move] = player.moves.filter{ m =>
      val (x1, y1) = m(x,y)
      x1 >= 0 && x1 < board.width && y1 >= 0 && y1 < board.length
    }
    choose(validMoves.iterator)
  }
}

object RandomMoveModel{
  def apply(): RandomMoveModel = new RandomMoveModel()
}