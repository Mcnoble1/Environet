package org.environet_metagraph.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.generic.auto._
import org.environet_metagraph.shared_data.types.GreenEnergy.GreenEnergyPlatformData
import org.environet_metagraph.shared_data.types.CarbonFootprint.CarbonFootprintData
import org.tessellation.currency.dataApplication.DataUpdate
import org.tessellation.schema.address.Address
import org.tessellation.schema.balance.Amount

object DataUpdates {
  
  // Base trait for all Environet updates
  @derive(encoder, decoder)
  sealed trait EnvironetUpdate extends DataUpdate {
    val address: Address
  }

  // Green energy participation update
  @derive(encoder, decoder)
  case class GreenEnergyParticipationUpdate(
    address          : Address,
    platformData     : GreenEnergyPlatformData
  ) extends EnvironetUpdate

  // Carbon footprint update
  @derive(encoder, decoder)
  case class CarbonFootprintUpdate(
    address         : Address,
    footprintData   : CarbonFootprintData
  ) extends EnvironetUpdate

  // Green energy investment update
  @derive(encoder, decoder)
  case class GreenEnergyInvestmentUpdate(
    address        : Address,
    investmentAmount: Amount,
    platformData   : GreenEnergyPlatformData
  ) extends EnvironetUpdate

  // General reward update for participants reducing their carbon footprint or using green energy
  @derive(encoder, decoder)
  case class GreenEnergyRewardUpdate(
    address       : Address,
    rewardAmount  : Amount,
    rewardReason  : String
  ) extends EnvironetUpdate

  // Additional environmental activity updates can be added here
}
