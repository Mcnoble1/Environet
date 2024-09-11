package org.environet.shared_data.validations

import org.environet.shared_data.Utils.toCarbonEquivalentFormat
import org.environet.shared_data.types.DataUpdates.{GreenEnergyParticipationUpdate, CarbonFootprintUpdate, InvestmentPlatformUpdate}
import org.environet.shared_data.validations.Errors._
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

object TypeValidators {

  // Validate that the participant meets the minimum green energy participation threshold
  def validateGreenEnergyParticipationThreshold(
    greenEnergyUpdate: GreenEnergyParticipationUpdate
  ): DataApplicationValidationErrorOr[Unit] = {
    val participationAmount = greenEnergyUpdate.participationAmount
    val minRequiredParticipation = toCarbonEquivalentFormat(100) // Example threshold for validation
    InsufficientGreenEnergyParticipation.unlessA(participationAmount >= minRequiredParticipation)
  }

  // Validate that the submitted carbon footprint data is within acceptable ranges
  def validateCarbonFootprintData(
    carbonFootprintUpdate: CarbonFootprintUpdate
  ): DataApplicationValidationErrorOr[Unit] = {
    val footprintData = carbonFootprintUpdate.footprintValue
    val maxAcceptableFootprint = toCarbonEquivalentFormat(500) // Example range for validation
    InvalidCarbonFootprintData.unlessA(footprintData <= maxAcceptableFootprint && footprintData >= 0)
  }

  // Validate that the investment platform meets Environet's sustainability criteria
  def validateInvestmentPlatformCompliance(
    platformName: String
  ): DataApplicationValidationErrorOr[Unit] = {
    // Example logic to check if platform is verified (pseudo-code)
    val verifiedPlatforms = Set("GreenInvest", "EcoCapital") // Example list of verified platforms
    UnverifiedInvestmentPlatform.unlessA(verifiedPlatforms.contains(platformName))
  }
}
