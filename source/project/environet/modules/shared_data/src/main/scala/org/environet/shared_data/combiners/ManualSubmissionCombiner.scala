package org.environet.shared_data.combiners

import cats.syntax.all._
import org.environet.shared_data.Utils.toTokenAmountFormat
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.ManualSubmission._
import org.environet.shared_data.types.States._
import org.tessellation.schema.address.Address
import org.tessellation.schema.epoch.EpochProgress

object ManualSubmissionCombiner {
  private val manualSubmissionRewardAmount: Long = 10L

  private def updateManualSubmissionDataSourceState(
    existing: ManualSubmissionDataSourceAddress,
    manualSubmissionUpdate: ManualSubmissionUpdate,
    currentEpochProgress: EpochProgress
  ): Map[Address, ManualSubmissionDataSourceAddress] = {
    val newTxnsIds = manualSubmissionUpdate.manualSubmissionTransactions.filterNot(existing.latestTransactionsIds.contains).map(_.id)
    if (newTxnsIds.isEmpty) {
      existing.addresses
    } else {
      val rewardsAmount = if (currentEpochProgress < existing.epochProgressToReward) {
        manualSubmissionRewardAmount * newTxnsIds.size + existing.amountToReward.value.value
      } else {
        manualSubmissionRewardAmount * newTxnsIds.size
      }
      existing.copy(
        amountToReward = toTokenAmountFormat(rewardsAmount),
        latestTransactionsIds = existing.latestTransactionsIds ++ newTxnsIds
      )
      .address
    }
  }

  def updateStateManualSubmission(
    currentCalculatedState: Map[DataSourceType, DataSource],
    currentEpochProgress: EpochProgress,
    manualSubmissionUpdate: ManualSubmissionUpdate
  ): ManualSubmissionDataSource = {
    val manualSubmissionDataSource = currentCalculatedState
      .get(DataSourceType.ManualSubmission)
      .collect { case manualSubmissionDataSource: ManualSubmissionDataSource => manualSubmissionDataSource }

    val updatedAddresses = manualSubmissionDataSource.fold {
      Map(
        manualSubmissionUpdate.address -> ManualSubmissionDataSourceAddress(
          currentEpochProgress,
          toTokenAmountFormat(manualSubmissionRewardAmount * manualSubmissionUpdate.manualSubmissionTransactions.size),
          manualSubmissionUpdate.manualSubmissionTransactions.map(_.id),
          Set.empty[String]
        )
      )
    } { dataSource =>
      updateManualSubmissionDataSourceState(dataSource.addresses(manualSubmissionUpdate.address), manualSubmissionUpdate, currentEpochProgress)
    }

    ManualSubmissionDataSource(updatedAddresses)
  }
}
