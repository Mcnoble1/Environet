package org.environet.shared_data.validations

import cats.syntax.all._
import org.tessellation.currency.dataApplication.DataApplicationValidationError
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

object Errors {
  private type DataApplicationValidationType = DataApplicationValidationErrorOr[Unit]

  val valid: DataApplicationValidationType = ().validNec[DataApplicationValidationError]

  implicit class DataApplicationValidationTypeOps[E <: DataApplicationValidationError](err: E) {
    def invalid: DataApplicationValidationType = err.invalidNec[Unit]

    def unlessA(cond: Boolean): DataApplicationValidationType = if (cond) valid else invalid

    def whenA(cond: Boolean): DataApplicationValidationType = if (cond) invalid else valid
  }

  // Validation errors for Environet

  case object InsufficientGreenEnergyParticipation extends DataApplicationValidationError {
    val message = "Participant must engage in a minimum threshold of green energy activities to qualify for rewards."
  }

  case object InvalidCarbonFootprintData extends DataApplicationValidationError {
    val message = "Provided carbon footprint data is invalid or not within acceptable ranges."
  }

  case object UnverifiedInvestmentPlatform extends DataApplicationValidationError {
    val message = "Investment platform is unverified or does not meet Environet's sustainability criteria."
  }

  case object ManualSubmissionRejected extends DataApplicationValidationError {
    val message = "Manual submission of green energy data was rejected due to invalid data or insufficient proof."
  }
}
