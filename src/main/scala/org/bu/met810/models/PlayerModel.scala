package org.bu.met810.models

trait PlayerModel[Env, Agent, Action]{
  def selectMove(assetId: Int, e: Env): Action
}