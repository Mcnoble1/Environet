package org.environet.shared_data.combiners

import cats.syntax.all._
import monocle.Monocle.toAppliedFocusOps
import org.environet.shared_data.Utils.toTokenAmountFormat
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.CarbonOffset._
import org.environet.shared_data.types.States._
import org.tessellation.schema.address.Address
import org.tessellation.schema.epoch.EpochProgress

object CarbonOffsetCombiner {
  private val carbonOffsetRewardAmount: Long = 20L

  private def getCurrentCarbonOffsetDataSource(
    currentCalculatedState: Map[DataSourceType, DataSource]
  ): CarbonOffsetDataSource = {
    currentCalculatedState
      .get(DataSourceType.CarbonOffset) match {
      case Some(carbonOffsetDataSource: CarbonOffsetDataSource) => carbonOffsetDataSource
      case _ => CarbonOffsetDataSource(Map.empty)
    }
  }

  private def updateCarbonOffsetDataSourceState(
    existing            : CarbonOffsetDataSourceAddress,
    carbonOffsetUpdate  : CarbonOffsetUpdate,
    currentEpochProgress: EpochProgress
  ): Map[Address, CarbonOffsetDataSourceAddress] = {
    val newTxnsIds = carbonOffsetUpdate.carbonOffsetTransactions.filterNot(existing.latestTransactionsIds.contains).map(_.id)
    if (newTxnsIds.isEmpty) {
      existing.addresses
    } else {
      val rewardsAmount = if (currentEpochProgress < existing.epochProgressToReward) {
        carbonOffsetRewardAmount * newTxnsIds.size + existing.amountToReward.value.value
      } else {
        carbonOffsetRewardAmount * newTxnsIds.size
      }
      existing.copy(
        amountToReward = toTokenAmountFormat(rewardsAmount),
        latestTransactionsIds = existing.latestTransactionsIds ++ newTxnsIds
      )
      .address
    }
  }

  def updateStateCarbonOffset(
    currentCalculatedState: Map[DataSourceType, DataSource],
    currentEpochProgress  : EpochProgress,
    carbonOffsetUpdate    : CarbonOffsetUpdate
  ): CarbonOffsetDataSource = {
    val carbonOffsetDataSource = getCurrentCarbonOffsetDataSource(currentCalculatedState)
    val updatedAddresses = carbonOffsetDataSource.addresses
      .get(carbonOffsetUpdate.address)
      .fold {
        val newDataSourceAddress = CarbonOffsetDataSourceAddress(
          currentEpochProgress,
          toTokenAmountFormat(carbonOffsetRewardAmount * carbonOffsetUpdate.carbonOffsetTransactions.size),
          carbonOffsetUpdate.carbonOffsetTransactions.map(_.id),
          Set.empty[String]
        )
        carbonOffsetDataSource.addresses.updated(carbonOffsetUpdate.address, newDataSourceAddress)
      } { existing =>
        updateCarbonOffsetDataSourceState(existing, carbonOffsetUpdate, currentEpochProgress)
      }
    CarbonOffsetDataSource(updatedAddresses)
  }
}
