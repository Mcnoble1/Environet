package org.environet.shared_data

import cats.effect.Async
import cats.syntax.all._
import org.environet.shared_data.app.ApplicationConfig
import org.environet.shared_data.combiners.Combiner.combineEnvironetUpdate
import org.environet.shared_data.combiners.GreenEnergyPlatformCombiner.cleanGreenEnergyAlreadyRewarded
import org.environet.shared_data.combiners.CarbonFootprintCalculatorCombiner.cleanCarbonFootprintAlreadyRewarded
import org.environet.shared_data.combiners.InvestmentPlatformCombiner.cleanInvestmentAlreadyRewarded
import org.environet.shared_data.combiners.ManualSubmissionCombiner.cleanManualSubmissionsAlreadyRewarded
import org.environet.shared_data.combiners.XCombiner.updateRewardsOlderThanOneDay
import org.environet.shared_data.types.DataUpdates.{EnvironetUpdate, GreenEnergyPlatformUpdate}
import org.environet.shared_data.types.States.{EnvironetCalculatedState, EnvironetOnChainState}
import org.environet.shared_data.validations.Errors.valid
import org.environet.shared_data.validations.Validations.greenEnergyPlatformValidationsL1
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.currency.dataApplication.{DataState, L0NodeContext}
import org.tessellation.ext.cats.syntax.next.catsSyntaxNext
import org.tessellation.schema.epoch.EpochProgress
import org.tessellation.security.signature.Signed
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object LifecycleSharedFunctions {
  def logger[F[_] : Async]: SelfAwareStructuredLogger[F] = Slf4jLogger.getLoggerFromName[F]("LifecycleSharedFunctions")

  def validateUpdate[F[_] : Async](
    update: EnvironetUpdate
  ): F[DataApplicationValidationErrorOr[Unit]] =
    update match {
      case greenEnergyUpdate: GreenEnergyPlatformUpdate =>
        Async[F].delay {
          greenEnergyPlatformValidationsL1(greenEnergyUpdate)
        }
      case _ => valid.pure[F]
    }

  def combine[F[_] : Async](
    oldState : DataState[EnvironetOnChainState, EnvironetCalculatedState],
    updates  : List[Signed[EnvironetUpdate]],
    appConfig: ApplicationConfig
  )(implicit context: L0NodeContext[F]): F[DataState[EnvironetOnChainState, EnvironetCalculatedState]] = {
    val newState = DataState(EnvironetOnChainState(List.empty), EnvironetCalculatedState(oldState.calculated.dataSources))
    for {
      epochProgress <- context.getLastCurrencySnapshot.flatMap {
        case Some(value) => value.epochProgress.next.pure[F]
        case None =>
          val message = "Could not get the epochProgress from currency snapshot. lastCurrencySnapshot not found"
          logger.error(message) >> new Exception(message).raiseError[F, EpochProgress]
      }
      response <- if (updates.isEmpty) {
        logger.info("Snapshot without any updates, updating the state to empty updates").as(
          newState
        )
      } else {
        for {
          _ <- logger.info(s"Incoming updates: ${updates.length}")
          combined <- updates.foldLeftM(newState) { (acc, signedUpdate) =>
            combineEnvironetUpdate(
              acc,
              epochProgress,
              signedUpdate,
              appConfig
            )
          }
        } yield combined
      }

      cleanedGreenEnergy = cleanGreenEnergyAlreadyRewarded(response.calculated.dataSources, epochProgress)
      cleanedCarbonFootprint = cleanCarbonFootprintAlreadyRewarded(cleanedGreenEnergy, epochProgress)
      cleanedInvestment = cleanInvestmentAlreadyRewarded(cleanedCarbonFootprint, epochProgress)
      cleanedManualSubmissions = cleanManualSubmissionsAlreadyRewarded(cleanedInvestment, epochProgress)
      updatedXRewardsOlderThanOneDay = updateRewardsOlderThanOneDay(cleanedManualSubmissions, epochProgress)
    } yield DataState(
      response.onChain,
      EnvironetCalculatedState(updatedXRewardsOlderThanOneDay)
    )
  }
}
