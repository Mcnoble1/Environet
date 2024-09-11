package org.environet.shared_data.combiners

import cats.effect.Async
import cats.syntax.all._
import org.environet.shared_data.app.ApplicationConfig
import org.environet.shared_data.combiners.GreenEnergyCombiner.updateStateGreenEnergy
import org.environet.shared_data.combiners.CarbonOffsetCombiner.updateStateCarbonOffset
import org.environet.shared_data.combiners.InvestmentCombiner.updateStateInvestment
import org.environet.shared_data.combiners.ManualSubmissionCombiner.updateStateManualSubmission
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.States._
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.schema.epoch.EpochProgress
import org.tessellation.security.signature.Signed
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object EnvironetCombiner {
  def combineEnvironetUpdate[F[_] : Async](
    oldState            : DataState[EnvironetOnChainState, EnvironetCalculatedState],
    currentEpochProgress: EpochProgress,
    update              : Signed[EnvironetUpdate],
    applicationConfig   : ApplicationConfig
  ): F[DataState[EnvironetOnChainState, EnvironetCalculatedState]] = {
    val currentDataSources = oldState.calculated.dataSources

    val (dataSourceType, updatedDataSourceF): (DataSourceType, F[DataSource]) = update.value match {
      case update: GreenEnergyUpdate =>
        implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("GreenEnergyCombiner")
        val greenEnergyDataSourceUpdated = updateStateGreenEnergy(
          currentDataSources,
          currentEpochProgress,
          update
        ).map(_.asInstanceOf[DataSource])

        (DataSourceType.GreenEnergy, greenEnergyDataSourceUpdated)

      case update: CarbonOffsetUpdate =>
        implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("CarbonOffsetCombiner")
        val carbonOffsetDataSourceUpdated = updateStateCarbonOffset(
          currentDataSources,
          currentEpochProgress,
          update
        ).map(_.asInstanceOf[DataSource])

        (DataSourceType.CarbonOffset, carbonOffsetDataSourceUpdated)

      case update: InvestmentUpdate =>
        implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("InvestmentCombiner")
        val investmentDataSourceUpdated = updateStateInvestment(
          currentDataSources,
          currentEpochProgress,
          update
        ).map(_.asInstanceOf[DataSource])

        (DataSourceType.Investment, investmentDataSourceUpdated)

      case update: ManualSubmissionUpdate =>
        implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("ManualSubmissionCombiner")
        val manualSubmissionDataSourceUpdated = updateStateManualSubmission(
          currentDataSources,
          currentEpochProgress,
          update
        ).map(_.asInstanceOf[DataSource])

        (DataSourceType.ManualSubmission, manualSubmissionDataSourceUpdated)
    }
    
    updatedDataSourceF.map { updatedDataSource =>
      val updates: List[EnvironetUpdate] = update.value :: oldState.onChain.updates
      DataState(
        EnvironetOnChainState(updates),
        EnvironetCalculatedState(
          currentDataSources.updated(
            dataSourceType,
            updatedDataSource
          )
        )
      )
    }
  }
}
