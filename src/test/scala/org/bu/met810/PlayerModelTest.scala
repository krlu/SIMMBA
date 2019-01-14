package org.bu.met810

import java.io.FileWriter

import org.bu.met810.data.CopsAndRobbersSim
import org.bu.met810.models.PlayerModel
import org.bu.met810.models.generative.{BayesianPlayerModel, DeterministicPlayerModel}
import org.bu.met810.types.copsandrobbersassets.{Board, Move, Player}
import org.scalatest.{FlatSpec, Matchers}

class PlayerModelTest extends FlatSpec with Matchers {

  "Random robber model" should "win infrequently" in {
    List(true, false).foreach { noise => println(CopsAndRobbersSim.runBatch(shouldApplyNoise = noise))}
  }

  val generative = "Generative"
  val deterministic = "Deterministic"
  val bayesian = "Bayesian"

  "Bayesian robber model" should "win often with Bayesian model" in {
    val builder1: (String, Boolean) => PlayerModel[Board, Player, Move]= DeterministicPlayerModel.apply
    val builder2: (String, Boolean) => PlayerModel[Board, Player, Move]= BayesianPlayerModel.apply
    val fw = new FileWriter("results.csv", true)
    fw.write("learnerType,iteratorType,trainedWithNoise,testWithNoise,trainingSize,modelName,robberWins,copWins,winPct\n")
    for{
      testWithNoise <- List(true, false)
      trainedWithNoise <- List(true, false)
      learnerType <- List(generative)
      iteratorType <- List(deterministic, bayesian)
      trainingSize <- List(2,4,8)
    }{
      val paramsFile = s"trainedModels/${learnerType}ModelLearner_${iteratorType}PlayerModel_${trainedWithNoise}_$trainingSize.json"
      val model =
        if(iteratorType == deterministic) builder1(paramsFile, learnerType == generative)
        else builder2(paramsFile, learnerType == generative)

      val modelName = model.getClass.toString.split('.').toList.last
      val trials = if (model.isInstanceOf[DeterministicPlayerModel]) 10000 else 1000
      val (robberWins, copWins) = CopsAndRobbersSim.runBatch(model, numTrials = trials, shouldApplyNoise = testWithNoise)
      val winPct = robberWins.toDouble / (robberWins + copWins)
      fw.write(List(learnerType, iteratorType, trainedWithNoise, testWithNoise,
        trainingSize, modelName, robberWins, copWins, winPct
      ).mkString(",") + "\n"
      )
    }
    fw.close()
  }

}

