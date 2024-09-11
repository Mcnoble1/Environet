package org.environet_metagraph.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import org.environet_metagraph.shared_data.types.CarbonFootprintCalculator.CarbonFootprintDataSource
import org.environet_metagraph.shared_data.types.GreenEnergyPlatform.GreenEnergyPlatformDataSource
import org.environet_metagraph.shared_data.types.InvestmentPlatform.InvestmentPlatformDataSource
import org.environet_metagraph.shared_data.types.ManualSubmission.ManualSubmissionDataSource
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState}
import org.tessellation.schema.address.Address

object States {
  implicit val config: Configuration = Configuration.default.withDefaults

  @derive(encoder, decoder)
  sealed abstract class DataSourceType(val value: String) extends StringEnumEntry

  object DataSourceType extends StringEnum[DataSourceType] with StringCirceEnum[DataSourceType] {
    val values = findValues

    case object GreenEnergyPlatform extends DataSourceType("GreenEnergyPlatform")

    case object CarbonFootprintCalculator extends DataSourceType("CarbonFootprintCalculator")

    case object InvestmentPlatform extends DataSourceType("InvestmentPlatform")

    case object ManualSubmission extends DataSourceType("ManualSubmission")
  }

  @derive(encoder, decoder)
  sealed trait DataSource

  @derive(encoder, decoder)
  case class GreenEnergyPlatformDataSource(
    addresses: Map[Address, GreenEnergyPlatformDataSource]
  ) extends DataSource

  @derive(encoder, decoder)
  case class CarbonFootprintDataSource(
    addresses: Map[Address, CarbonFootprintDataSource]
  ) extends DataSource

  @derive(encoder, decoder)
  case class InvestmentPlatformDataSource(
    addresses: Map[Address, InvestmentPlatformDataSource]
  ) extends DataSource

  @derive(encoder, decoder)
  case class ManualSubmissionDataSource(
    addresses: Map[Address, ManualSubmissionDataSource]
  ) extends DataSource

  @derive(encoder, decoder)
  case class EnvironetOnChainState(
    updates: List[DataUpdate]  // Replace DataUpdate with the relevant case class for Environet updates
  ) extends DataOnChainState

  @derive(encoder, decoder)
  case class EnvironetCalculatedState(
    dataSources: Map[DataSourceType, DataSource]
  ) extends DataCalculatedState
}
