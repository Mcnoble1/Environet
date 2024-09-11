package org.environet.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.schema.balance.Amount
import org.tessellation.schema.epoch.EpochProgress

object CarbonFootprintCalculator {
  @derive(encoder, decoder)
  case class CarbonFootprintDataSource(
    epochProgressToReward: EpochProgress,
    amountToReward: Amount,
    calculations: List[FootprintCalculation]
  )

  case class FootprintCalculation(
    calculationId: String,
    carbonSaved: Double,
    calculationDate: String
  )
}
