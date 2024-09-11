package org.environet_metagraph.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.schema.balance.Amount
import org.tessellation.schema.epoch.EpochProgress

object InvestmentPlatform {
  @derive(encoder, decoder)
  case class InvestmentPlatformDataSource(
    epochProgressToReward: EpochProgress,
    amountToReward: Amount,
    latestInvestments: Set[String],
    olderInvestments: Set[String]
  )

  case class InvestmentInfo(
    investmentId: String,
    investmentName: String,
    amount: Double,
    date: String
  )

  case class InvestmentPlatformApiResponse(data: List[InvestmentInfo])
}
