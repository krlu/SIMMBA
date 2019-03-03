package org.bu.met810

import java.io.PrintWriter

import org.bu.met810.data.{CopsAndRobbersSim, DataGenerator, SimBuilder, Simulator}
import org.bu.met810.models.PlayerModel
import org.bu.met810.models.generative.{BayesianPlayerModel, DeterministicPlayerModel}
import org.bu.met810.models.learners.{BayesianModelLearner, GenerativeModelLearner, Learner}
import org.bu.met810.models.random.RandomMoveModel
import org.bu.met810.types.{Agent, Environment, Vectorizable}
import org.bu.met810.types.copsandrobbersassets.{Move, _}


object HillClimbingExperiment {

  def main(args: Array[String]): Unit = {
    /** Experiment for Cops and Robbers */
    val ROBBER_ID = 0
    val COP_ID = 1
    val isValidState: Seq[Int] => Boolean = state  => state(1) != state(3) || state(2) != state(4)

    def vectorToBoard(vector: Seq[Int]): Board = {
      val p1 = Robber((vector.head, vector(1)))
      val p2 = Cop((vector(2), vector(3)))
      Board(p1, p2, vector(4), vector(5), Seq())
    }
    def vectorToMove(vector: Seq[Int]): Move =
      Move.possibleMoves.find(_.toVector == vector.map(_.ceil.toInt)) match {
      case Some(move) => move
      case None => throw new NoSuchElementException(s"unable to find move with vector ${vector.map(_.ceil.toInt)}!")
    }
    runAllExperiments(Move.robberMoves, Move.copMoves, vectorToBoard, vectorToMove,
      CopsAndRobbersSim, agentDim = 2, Array(ROBBER_ID, COP_ID), isValidState)
  }

  def runAllExperiments[Env <: Environment[Action, A] with Vectorizable, A <: Agent with Vectorizable, Action <: Vectorizable](
    p1Moves: List[Action],
    p2Moves: List[Action],
    vectorToBoard: Seq[Int] => Env,
    vectorToMove: Seq[Int] => Action,
    simBuilder: SimBuilder[Env, A, Action],
    agentDim: Int, ids: Array[Int],
    isValidState: Seq[Int] => Boolean): Unit = {

    require(ids.length == 2)
    val iter1: (String, Boolean) => PlayerModel[Env, A, Action]= DeterministicPlayerModel.apply(_, _, p1Moves)
    val iter2: (String, Boolean) => PlayerModel[Env, A, Action] = BayesianPlayerModel.apply(_, _, p1Moves)
    val paramsFile = s"temp_model.json"
    val playerId = 0
    val boardSize = 4
    for{
      iterateWithNoise <- List(false)
      trainingSize <- List(1000)
      learner <- List(
        new GenerativeModelLearner[Env, A, Action](vectorToBoard,
          vectorToMove, agentDim, isValidState, (p1Moves ++ p2Moves).distinct)
        ,new BayesianModelLearner[Env, A, Action](vectorToBoard, vectorToMove, paramsFile,
          useGenerativeParams = true, agentDim, isValidState, (p1Moves ++ p2Moves).distinct))
      iterationModelBuilder <- List(iter1, iter2)
    } {
      runOneExperiment(playerId, ids.length, learner, iterationModelBuilder, iterateWithNoise, trainingSize)
    }

    def runOneExperiment(playerIdToTrainFor: Int,
                         numPlayers: Int,
                         learner: Learner[Env, A, Action],
                         iterationModelBuilder: (String, Boolean) => PlayerModel[Env, A, Action],
                         iterateWithNoise: Boolean,
                         numTrainingSamples: Int): Unit = {
      require(numPlayers == 2)
      var maxWins = 0
      val trainingFile = s"training_data_$boardSize.csv"
      val useGenerativeParams = learner.isInstanceOf[GenerativeModelLearner[Board, Player, Move]]

      for(_ <- 1 to 300) {
        val pw = new PrintWriter(trainingFile)
        pw.write("")

        var start = System.currentTimeMillis()
        DataGenerator.generateData[Env, A, Action](
          trainingFile, boardSize, numTrainingSamples, playerIdToTrainFor, simBuilder,
          new RandomMoveModel[Env, A, Action](p1Moves), new RandomMoveModel[Env, A, Action](p2Moves))
        var end = System.currentTimeMillis()
        println(s"Data generation time: ${(end - start)/1000.0}s")

        start = System.currentTimeMillis()
        println("training player model...")
        learner.learn(trainingFile, boardSize, numPlayers, playerId = playerIdToTrainFor, paramsFile)
        end = System.currentTimeMillis()
        println(s"Training time: ${(end - start)/1000.0}s")

        val p1Model: PlayerModel[Env, A, Action] = iterationModelBuilder(paramsFile, useGenerativeParams)
        val p2Model: PlayerModel[Env, A, Action] = new RandomMoveModel[Env, A, Action](p2Moves)
        val modelName = p1Model.getClass.toString.split('.').toList.last
        val learnerName = learner.getClass.toString.split('.').toList.last

        start = System.currentTimeMillis()
        val winners = simBuilder.runBatch(p1Model, p2Model, shouldApplyNoise = iterateWithNoise)
        end = System.currentTimeMillis()
        println(s"runtime: ${(end - start).toDouble/1000}")

        val numP1Wins = winners.count(_.id == ids(0))
        val numP2Wins =  winners.count(_.id == ids(1))
        val totalWins = winners.size
        val wins = if(playerIdToTrainFor == ids(0)) numP1Wins else numP2Wins
        if(wins > maxWins) {
          maxWins = wins
          println(maxWins, totalWins, maxWins.toDouble/totalWins)
          val pw = new PrintWriter(s"${learnerName}_${modelName}_${iterateWithNoise}_$numTrainingSamples.json")
          val savedParams = scala.io.Source.fromFile(paramsFile).mkString
          pw.write(savedParams)
          pw.close()
        }
      }
    }
  }
}
