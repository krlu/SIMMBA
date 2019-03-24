package org.bu.met810.models.generative

import neuroflow.application.plugin.IO.File
import neuroflow.application.plugin.Notation.->
import neuroflow.core._
import neuroflow.dsl._
import neuroflow.nets.cpu.DenseNetwork._
import org.bu.met810.{NNVector, Turn, WinnerId, getTrainingData}
import org.bu.met810.models.PlayerModel
import org.bu.met810.types.{Agent, Environment}

/**
  * This class is different in that it is self-trainable.
  * Therefore there is no learner used to generate the parameters for this class
  * Reason is that a Neural Net is purely domain agnostic
  */
class NNPlayerModel[Env <: Environment[Action, A], A <: Agent, Action](
                                          inputDim: Int,
                                          outputDim: Int,
                                          val paramsFile: Option[String],
                                          val vectorToMove: Seq[Int] => Action) extends PlayerModel[Env, A, Action]{
  //  private val f1 = Activators.Double.Sigmoid
  private val f2 = Activators.Double.Linear

  implicit val weights: WeightBreeder[Double] = paramsFile match {
    case None => WeightBreeder[Double].normal(μ = 0.0, σ = 2.0)
    case Some(filePath) => File.weightBreeder(filePath)
  }
  val net = Network(
    layout = Vector (inputDim) :: Dense  (outputDim, f2)  ::  SquaredError(),
    settings = Settings[Double](
      updateRule = Vanilla(),
      batchSize = Some(100),
      iterations = 10000,
      learningRate = {
        case (iter, _) if iter < 128 => 0.00001
        case (_, _)  => 0.00001
      },
      precision = 1E-4
    )
  )

  def learn(trainingDataFilePath: String, paramsFile: String): Unit = {
    val (xs, ys) = getNNTrainingData(trainingDataFilePath).map{ case (x,y, _, _) => (x,y)}.unzip[NNVector, NNVector]
    net.train(xs, ys)
    File.writeWeights(net.weights, paramsFile)
  }

  def getNNTrainingData(filePath: String, boardDim: Int = 6, moveDim: Int = 2):
  List[(NNVector, NNVector, Turn, WinnerId)] =
    getTrainingData(filePath, boardDim, moveDim).map{ case (boardVec, moveVec, turn, winner) =>
      (->(boardVec.map(_.toDouble):_*), ->(moveVec.map(_.toDouble):_*), turn, winner)
    }

  override def selectMove(agent: A, e: Env): Action = {
    val stateVector = List(agent.id.toDouble)
    vectorToMove(net.evaluate(->(stateVector:_*)).toArray.toList.map(_.round.toInt))
  }
}
