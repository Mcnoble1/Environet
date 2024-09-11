package org.environet.shared_data.combiners

import cats.effect.Async
import cats.syntax.all._
import org.environet.shared_data.Utils.toTokenAmountFormat
import org.environet.shared_data.types.DataUpdates.GreenEnergyUpdate
import org.environet.shared_data.types.GreenEnergy._
import org.environet.shared_data.types.States._
import org.tessellation.schema.address.Address
import org.tessellation.schema.epoch.EpochProgress
import org.typelevel.log4cats.Logger

object GreenEnergyCombiner {
  private val greenEnergyRewardAmount: Long = 50L

  private def getNewTransactionsIds(
    existing    : GreenEnergyDataSourceAddress,
    greenEnergyUpdate: GreenEnergyUpdate
  ): Set[String] = {
    val existingTxnsIds = existing.latestTransactionsIds ++ existing.olderTransactionsIds
    greenEnergyUpdate.greenEnergyTransactions.filterNot(txn => existingTxnsIds.contains(txn.id)).map(_.id)
  }

  private def calculateRewardsAmount(
    existing            : GreenEnergyDataSourceAddress,
    newTxnsIds          : Set[String],
    currentEpochProgress: EpochProgress): Long = {
    if (currentEpochProgress < existing.epochProgressToReward) {
      (greenEnergyRewardAmount * newTxnsIds.size) + existing.amountToReward.value.value
    } else {
      greenEnergyRewardAmount * newTxnsIds.size
    }
  }

  private def updateGreenEnergyDataSourceState[F[_] : Async : Logger](
    existing               : GreenEnergyDataSourceAddress,
    greenEnergyUpdate      : GreenEnergyUpdate,
    currentGreenEnergyDataSource: GreenEnergyDataSource,
    currentEpochProgress   : EpochProgress
  ): F[Map[Address, GreenEnergyDataSourceAddress]] = {
    val newTxnsIds = getNewTransactionsIds(existing, greenEnergyUpdate)

    if (newTxnsIds.isEmpty) {
      currentGreenEnergyDataSource.addresses.pure[F]
    } else {
      val rewardsAmount = calculateRewardsAmount(existing, newTxnsIds, currentEpochProgress)

      val updatedGreenEnergyDataSource = GreenEnergyDataSourceAddress(
        currentEpochProgress,
        toTokenAmountFormat(rewardsAmount),
        newTxnsIds,
        existing.latestTransactionsIds ++ existing.olderTransactionsIds
      )

      Logger[F].info(s"Updated GreenEnergyDataSource for address ${greenEnergyUpdate.address}").as(
        currentGreenEnergyDataSource.addresses.updated(greenEnergyUpdate.address, updatedGreenEnergyDataSource)
      )
    }
  }

  private def getGreenEnergyDataSourceUpdatedAddresses[F[_] : Async : Logger](
    state        : Map[DataSourceType, DataSource],
    greenEnergyUpdate : GreenEnergyUpdate,
    epochProgress: EpochProgress
  ): F[Map[Address, GreenEnergyDataSourceAddress]] = {
    val greenEnergyDataSourceAddress = GreenEnergyDataSourceAddress(
      epochProgress,
      toTokenAmountFormat(greenEnergyRewardAmount * greenEnergyUpdate.greenEnergyTransactions.size),
      greenEnergyUpdate.greenEnergyTransactions.map(_.id),
      Set.empty[String]
    )

    state
      .get(DataSourceType.GreenEnergy)
      .fold(Map(greenEnergyUpdate.address -> greenEnergyDataSourceAddress).pure[F]) {
        case greenEnergyDataSource: GreenEnergyDataSource =>
          greenEnergyDataSource.addresses
            .get(greenEnergyUpdate.address)
            .fold(greenEnergyDataSource.addresses.updated(greenEnergyUpdate.address, greenEnergyDataSourceAddress).pure[F]) { existing =>
              updateGreenEnergyDataSourceState(existing, greenEnergyUpdate, greenEnergyDataSource, epochProgress)
            }
        case _ => new IllegalStateException("DataSource is not from type GreenEnergyDataSource").raiseError[F, Map[Address, GreenEnergyDataSourceAddress]]
      }
  }

  def updateStateGreenEnergy[F[_] : Async : Logger](
    currentCalculatedState: Map[DataSourceType, DataSource],
    currentEpochProgress  : EpochProgress,
    greenEnergyUpdate     : GreenEnergyUpdate
  ): F[GreenEnergyDataSource] =
    getGreenEnergyDataSourceUpdatedAddresses(currentCalculatedState, greenEnergyUpdate, currentEpochProgress).map { updatedAddresses =>
      GreenEnergyDataSource(updatedAddresses)
    }
}
