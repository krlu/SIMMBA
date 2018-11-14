package org.bu.met810

import org.bu.met810.data.Simulator
import org.bu.met810.model.bayes.BayesianPlayerModel
import org.bu.met810.model.{PlayerModel, RandomMoveModel}
import org.bu.met810.types.boardassets._
import org.bu.met810.types.moves.Move
import org.scalatest.{FlatSpec, Matchers}

class PlayerModelTest extends FlatSpec with Matchers {


  "Random robber model" should "win infrequently" in {
    println(runExperiment(RandomMoveModel()))
  }

  "Bayesian robber model" should "win often" in {
    val model = new BayesianPlayerModel("current_best_params.json")
    println(runExperiment(model))
  }

  private def runExperiment(model: PlayerModel[Board, Player, Move]): (Int, Int) = {
    val boardSize = 4
    val positions = 0 until boardSize
    val start = System.currentTimeMillis()
    val winners: Seq[Player] = for(i <- 1 to 1000) yield {
      val rX = choose(positions.iterator)
      val rY = choose(positions.iterator)
      val cX = choose(positions.filter(_ != rX).iterator)
      val cY = choose(positions.filter(_ != rY).iterator)
      val board = Board(Robber((rX, rY)), Cop((cX, cY)), boardSize, boardSize, Seq.empty[Building])
      val sim = Simulator(board, model, RandomMoveModel())
      println(i)
      sim.runFullGame()
    }
    val robbers = winners.filter(p => p.isInstanceOf[Robber])
    val cops = winners.filter(p => p.isInstanceOf[Cop])
    val end = System.currentTimeMillis()
    println(s"runtime: ${(end - start)/1000}")
    (robbers.size, cops.size)
  }
}

