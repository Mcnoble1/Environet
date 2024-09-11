package org.environet.shared_data.validations

import org.environet.shared_data.types.DataUpdates.{GreenEnergyParticipationUpdate, CarbonFootprintUpdate}
import org.environet.shared_data.validations.TypeValidators._
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

object Validations {

  // Validate that green energy participation meets the required thresholds
  def greenEnergyParticipationValidationsL1(
    greenEnergyUpdate: GreenEnergyParticipationUpdate
  ): DataApplicationValidationErrorOr[Unit] =
    validateGreenEnergyParticipationThreshold(greenEnergyUpdate)

  // Validate that carbon footprint data is within acceptable limits
  def carbonFootprintValidationsL1(
    carbonFootprintUpdate: CarbonFootprintUpdate
  ): DataApplicationValidationErrorOr[Unit] =
    validateCarbonFootprintData(carbonFootprintUpdate)

  // Additional validations for Environet participants can be added here
}
