package org.environet.shared_data.combiners

import cats.syntax.all._
import org.environet.shared_data.Utils.toTokenAmountFormat
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.Investment._
import org.environet.shared_data.types.States._
import org.tessellation.schema.address.Address
import org.tessellation.schema.epoch.EpochProgress

object InvestmentCombiner {
  private val investmentRewardAmount: Long = 100L

  private def getInvestmentDataSourceUpdatedAddresses(
    state: Map[DataSourceType, DataSource],
    investmentUpdate: InvestmentUpdate,
    epochProgress: EpochProgress
  ): Map[Address, InvestmentDataSourceAddress] = {
    val investmentDataSource = state
      .get(DataSourceType.Investment)
      .collect { case investmentDataSource: InvestmentDataSource => investmentDataSource }

    investmentDataSource.fold {
      Map(
        investmentUpdate.address -> InvestmentDataSourceAddress(
          epochProgress,
          toTokenAmountFormat(investmentRewardAmount * investmentUpdate.investmentTransactions.size),
          investmentUpdate.investmentTransactions.map(_.id),
          Set.empty[String]
        )
      )
    } { dataSource =>
      val newTxnsIds = investmentUpdate.investmentTransactions.filterNot(txn => dataSource.addresses(txn.address).latestTransactionsIds.contains(txn.id)).map(_.id)

      if (newTxnsIds.isEmpty) {
        dataSource.addresses
      } else {
        val rewardsAmount = investmentRewardAmount * newTxnsIds.size
        dataSource.copy(
          addresses = dataSource.addresses.updated(
            investmentUpdate.address,
            InvestmentDataSourceAddress(
              epochProgress,
              toTokenAmountFormat(rewardsAmount),
              newTxnsIds,
              dataSource.addresses(investmentUpdate.address).latestTransactionsIds ++ newTxnsIds
            )
          )
        ).addresses
      }
    }
  }

  def updateStateInvestment(
    currentCalculatedState: Map[DataSourceType, DataSource],
    currentEpochProgress: EpochProgress,
    investmentUpdate: InvestmentUpdate
  ): InvestmentDataSource = {
    InvestmentDataSource(getInvestmentDataSourceUpdatedAddresses(currentCalculatedState, investmentUpdate, currentEpochProgress))
  }
}
