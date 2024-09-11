package org.environet_metagraph.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.schema.address.Address
import org.tessellation.schema.balance.Amount
import org.tessellation.schema.epoch.EpochProgress

object ManualSubmission {
  @derive(encoder, decoder)
  case class ManualSubmissionDataSource(
    address: Address,
    submissionType: String,
    submissionDetails: String,
    epochProgressToReward: EpochProgress,
    amountToReward: Amount
  )

  case class ManualSubmissionApiResponse(data: List[ManualSubmissionDataSource])
}
