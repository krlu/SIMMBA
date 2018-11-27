package org.bu.met810.models.inference

import java.io.PrintWriter

import argonaut.Argonaut._
import com.cra.figaro.algorithm.learning.EMWithVE
import com.cra.figaro.language.{Select, Universe}
import com.cra.figaro.library.atomic.continuous.{AtomicDirichlet, Dirichlet}
import com.cra.figaro.patterns.learning.ModelParameters
import org.bu.met810.types.moves.{Move, _}
import org.bu.met810._
import org.bu.met810.models.JsonModelLoader


/** Given the dimensions of the board we build up our initial distributions
  * @param numRows - number of rows on the board
  * @param numCols - number of columns on the board
  * @param numPlayers - number of active players
  */
class BayesianModelLearner(numRows: Int, numCols: Int, numPlayers: Int = 2, playerId: Int = 0,
                           val paramsFile: String, useLearnedParams: Boolean) extends JsonModelLoader{

  override val useGenerativeParams: Boolean = false

  val allMoves = List(Up, Down, Left, Right, SkipUp, SkipDown, SkipLeft, SkipRight)

  Universe.createNew()
  val modelParams = ModelParameters()

  private def possiblePositions(): List[(Int, Int)] =
    {for{
      x <- 0 until numCols
      y <- 0 until numRows
    }yield(x,y)}.toList

  val initialMoveParams: Seq[AtomicDirichlet] =
  if(useLearnedParams) paramsMap.map{case(k,v) => Dirichlet(v:_*)(k, modelParams)}.toList
  else {
    permutationsWithRepetitions(possiblePositions(), numPlayers).map{ positions =>
      val name = s"${playerId}_${positions.flatMap{case(a,b) => List(a,b)}.mkString("_")}_move"
      Dirichlet(Array.fill(allMoves.size)(1.0):_*)(name, modelParams)
    }
  }

  def train(data: List[(List[(Int, Int)], Move)]): String = {
    data.foreach{ case(p, m) => generateTrial(p,m)}
    val alg = EMWithVE(2, modelParams)
    alg.start()
    val savedParams = modelParams.asJson.toString()
    alg.stop()
    alg.kill()
    savedParams
  }

  private def generateTrial(positions: List[(Int, Int)], move: Move): Unit = {
    val posVector = positions.flatMap{case (x,y) => List(x,y)}
    val params = modelParams.getElementByReference(s"${playerId}_${posVector.mkString("_")}_move").asInstanceOf[AtomicDirichlet]
    val moveDist = Select(params, allMoves:_*)
    moveDist.observe(move)
  }
}

object BayesianModelLearner extends Learner{

  def learn(trainingDataFilePath: String, boardSize: Int, numPlayers: Int, playerId: Int, paramsFile: String): Unit = {
    val numRows = boardSize
    val numCols = boardSize
    val boardDim = numPlayers * 2 + 2 //num players * num coordinates + 1 for board length + 1 for board width
    val moveDim = 2

    def trainForPlayer(playerId: Int): Unit = {
      val data = getFeaturizedTrainingData(trainingDataFilePath, boardDim, moveDim)
      val playerSpecificData: List[(List[(Int, Int)], Move)] = data.filter{ case (_, _, turn, winnerId) =>
        turn == playerId && winnerId == playerId
      }.map{ case (board, move, _, _) =>
        (List(board.p1.position, board.p2.position), move)
      }.toList

      val pml = new BayesianModelLearner(numRows, numCols, numPlayers, playerId, paramsFile, useLearnedParams = false)
      val paramsString = pml.train(playerSpecificData)
      val pw = new PrintWriter(paramsFile)
      pw.println(paramsString)
      pw.close()
    }

    val start = System.currentTimeMillis()
    trainForPlayer(playerId)
    val end = System.currentTimeMillis()
    println(s"Training time: ${(end - start)/1000.0}s")
  }
}
