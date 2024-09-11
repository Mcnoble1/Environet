package org.environet.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object ExistingParticipants {
  @derive(encoder, decoder)
  case class ExistingParticipantDataSource(
    greenEnergyRewarded: Boolean,
    footprintReductionRewarded: Boolean
  )

  object ExistingParticipantDataSource {
    def empty: ExistingParticipantDataSource = {
      ExistingParticipantDataSource(greenEnergyRewarded = false, footprintReductionRewarded = false)
    }
  }
}
