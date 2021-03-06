package org.bu.simmba.simulation

import java.io.{File, FileWriter}

import org.bu.simmba.models.PlayerModel
import org.bu.simmba.types.{Agent, Environment, Vectorizable}
import org.bu.simmba.{Turn, WinnerId}


/**
  * If the player with id=PlayerId wins, we save all moves from that game as training data
  */
object DataGenerator{
  /**
    * @param outputFilePath - csv file to contain training data
    * @param boardSize - rows and columns of a square board
    * @param numSamples - number of samples per unique board state
    * @param playerId - Id of agent to generate data for
    * @param simBuilder - generic builder for simulator
    * @param p1Model - move model for p1 assuming 2 players total
    * @param p2Model - move model for p2 assuming 2 players total
    * @tparam Env - represents game environment
    * @tparam A - represents agents interacting with game environment
    * @tparam Action - represents action an agent can take
    */

  def generateData[Env <: Environment[Action, A] with Vectorizable, A <: Vectorizable with Agent, Action <: Vectorizable](
                                                outputFilePath: String, boardSize: Int, numSamples: Int,
                                                playerId: Int, simBuilder: SimBuilder[Env, A, Action],
                                                p1Model: PlayerModel[Env, A, Action],
                                                p2Model: PlayerModel[Env, A, Action]): Unit = {
    for(_ <- 0 until numSamples) {
      val state = simBuilder.randomInitialization(p1Model, p2Model, boardSize)
      generateDataPoint(playerId, outputFilePath, state)
    }
  }

  private def generateDataPoint[Env <: Vectorizable with Environment[Action, A], A <: Vectorizable with Agent, Action <: Vectorizable]
  (playerId: Int, outputFilePath: String, sim: TurnBasedSimulator[Env, A, Action]): Unit = {
    var data = List.empty[(Env, Action, Turn)]
    var result: Option[(Env, Action, Env)] = None
    while(!sim.isGameOver){
      val prevTurn = sim.getTurn
      result = sim.runStep()
      if(result.nonEmpty) {
        val (prevState, action, _) = result.get
        data = data :+ (prevState, action, prevTurn)
      }
    }
    val winnerId: WinnerId = sim.getWinner.get.id
    if(winnerId == playerId) {
      data.foreach { case (state, action, turn) =>
        saveVectors(outputFilePath, state.toVector, action.toVector, turn, winnerId)
      }
    }
  }

  private def saveVectors(filePath: String, stateVec: Seq[Double], moveVec: Seq[Double], turn: Int, winnerId: WinnerId): Unit ={
    val pw = new FileWriter(new File(filePath), true)
    pw.append(s"${stateVec.mkString(",")},${moveVec.mkString(",")},$turn,$winnerId \n")
    pw.close()
  }
}
