package org.environet.l0.rewards

import cats.effect.Async
import cats.syntax.all._
import eu.timepit.refined.types.numeric.PosLong
import org.environet.shared_data.Utils.PosLongOps
import org.environet.shared_data.types.States.DataSourceType._
import org.environet.shared_data.types.States._
import org.tessellation.currency.dataApplication.DataCalculatedState
import org.tessellation.currency.schema.currency.{CurrencyIncrementalSnapshot, CurrencySnapshotStateProof}
import org.tessellation.ext.cats.syntax.next.catsSyntaxNext
import org.tessellation.node.shared.domain.rewards.Rewards
import org.tessellation.node.shared.infrastructure.consensus.trigger.{ConsensusTrigger, EventTrigger, TimeTrigger}
import org.tessellation.node.shared.snapshot.currency.CurrencySnapshotEvent
import org.tessellation.schema.address.Address
import org.tessellation.schema.balance.{Amount, Balance}
import org.tessellation.schema.epoch.EpochProgress
import org.tessellation.schema.transaction.{RewardTransaction, Transaction, TransactionAmount}
import org.tessellation.security.signature.Signed
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.collection.immutable.{Map, SortedMap, SortedSet}

object EnvironetRewards {
  implicit class RewardTransactionOps(tuple: (Address, PosLong)) {
    def toRewardTransaction: RewardTransaction = {
      val (address, amount) = tuple
      RewardTransaction(address, TransactionAmount(amount))
    }
  }

  def make[F[_] : Async](): Rewards[F, CurrencySnapshotStateProof, CurrencyIncrementalSnapshot, CurrencySnapshotEvent] =
    (
      lastArtifact        : Signed[CurrencyIncrementalSnapshot],
      _                   : SortedMap[Address, Balance],
      _                   : SortedSet[Signed[Transaction]],
      trigger             : ConsensusTrigger,
      _                   : Set[CurrencySnapshotEvent],
      maybeCalculatedState: Option[DataCalculatedState]
    ) => {
      val logger = Slf4jLogger.getLoggerFromName[F]("EnvironetRewards")

      def noRewards: F[SortedSet[RewardTransaction]] = SortedSet.empty[RewardTransaction].pure[F]

      def getAddressAndAmounts(
        state               : EnvironetCalculatedState,
        currentEpochProgress: EpochProgress,
        dataSourceType      : DataSourceType
      ): Map[Address, Amount] = {
        state.dataSources.get(dataSourceType).fold(Map.empty[Address, Amount]) {
          case dataSource: GreenEnergyPlatformDataSource =>
            dataSource.addresses.collect {
              case (address, ds) if ds.epochProgressToReward === currentEpochProgress => address -> ds.amountToReward
            }
          case dataSource: CarbonFootprintCalculatorDataSource =>
            dataSource.addresses.collect {
              case (address, ds) if ds.epochProgressToReward === currentEpochProgress => address -> ds.amountToReward
            }
          case dataSource: InvestmentPlatformDataSource =>
            dataSource.addresses.collect {
              case (address, ds) if ds.epochProgressToReward === currentEpochProgress => address -> ds.amountToReward
            }

          case dataSource: ManualSubmissionDataSource =>
            dataSource.addressesToReward.collect {
              case (address, ds) if ds.epochProgressToReward.contains(currentEpochProgress) => address -> ds.amountToReward
            }

          case _ => Map.empty[Address, Amount]
        }
      }

      def buildRewards(
        calculatedState   : EnvironetCalculatedState,
        currentEpochProgress: EpochProgress
      ): F[SortedSet[RewardTransaction]] = for {
        _ <- logger.info("Starting to build the rewards")
        combinedAddressesAndAmounts = Seq(GreenEnergyPlatform, CarbonFootprintCalculator, InvestmentPlatform, ManualSubmission)
          .flatMap(getAddressAndAmounts(calculatedState, currentEpochProgress, _))
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2.value.value).sum)
          .toMap

        transactions <- combinedAddressesAndAmounts.foldLeft(SortedSet.empty[RewardTransaction].pure[F]) {
          (rewardTransactionsF, currentAddressInfo) =>
            rewardTransactionsF.flatMap { rewardTransactions =>
              val (address, amountToReward) = currentAddressInfo
              if (amountToReward == 0) rewardTransactions.pure
              else {
                val rewardTransaction = (address, amountToReward.toPosLongUnsafe).toRewardTransaction
                logger.info(s"Address $address will be rewarded with $amountToReward in ${currentEpochProgress.show}")
                  .as(rewardTransactions + rewardTransaction)
              }
            }
        }
      } yield transactions

      trigger match {
        case EventTrigger => noRewards
        case TimeTrigger =>
          val currentEpochProgress: EpochProgress = lastArtifact.epochProgress.next
          maybeCalculatedState.fold(noRewards) {
            case calculatedState: EnvironetCalculatedState =>
              buildRewards(calculatedState, currentEpochProgress)
            case _ => logger.error("Invalid calculated state class") >> noRewards
          }
      }
    }
}
