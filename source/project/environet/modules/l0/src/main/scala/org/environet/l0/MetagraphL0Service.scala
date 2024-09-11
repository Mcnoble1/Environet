package org.environet.l0

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all._
import eu.timepit.refined.refineV
import eu.timepit.refined.types.numeric.NonNegLong
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.environet.l0.custom_routes.CustomRoutes
import org.environet.shared_data.LifecycleSharedFunctions
import org.environet.shared_data.Utils.toTokenAmountFormat
import org.environet.shared_data.app.ApplicationConfig
import org.environet.shared_data.calculated_state.CalculatedStateService
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.GreenEnergyPlatforms.{GreenEnergyPlatformDataSourceAddress}
import org.environet.shared_data.types.CarbonFootprintCalculators.CarbonFootprintCalculatorDataSourceAddress
import org.environet.shared_data.types.InvestmentPlatforms.InvestmentPlatformDataSourceAddress
import org.environet.shared_data.types.States._
import org.environet.shared_data.types.codecs.DataUpdateCodec._
import org.environet.shared_data.validations.Errors.valid
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.json.JsonSerializer
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.address.{Address, DAGAddressRefined}
import org.tessellation.schema.epoch.EpochProgress
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import scala.io.Source

object MetagraphL0Service {

  def make[F[+_] : Async : JsonSerializer](
    calculatedStateService: CalculatedStateService[F],
    applicationConfig     : ApplicationConfig
  ): F[BaseDataApplicationL0Service[F]] = Async[F].delay {
    makeBaseDataApplicationL0Service(
      calculatedStateService,
      applicationConfig
    )
  }

  private def makeBaseDataApplicationL0Service[F[+_] : Async : JsonSerializer](
    calculatedStateService: CalculatedStateService[F],
    applicationConfig     : ApplicationConfig
  ): BaseDataApplicationL0Service[F] =
    BaseDataApplicationL0Service(
      new DataApplicationL0Service[F, EnvironetUpdate, EnvironetOnChainState, EnvironetCalculatedState] {
        def readLinesFromFile(fileName: String): List[String] = {
          val source = Source.fromResource(fileName)
          try {
            source.getLines().toList
          } finally {
            source.close()
          }
        }

        def buildDataSourceAddresses(registeredWallets: List[Address]): (Map[Address, GreenEnergyPlatformDataSourceAddress], Map[Address, CarbonFootprintCalculatorDataSourceAddress], Map[Address, InvestmentPlatformDataSourceAddress]) = {
          val greenEnergyPlatformAddresses = registeredWallets.foldLeft(Map.empty[Address, GreenEnergyPlatformDataSourceAddress]) { (acc, address) =>
            acc.updated(address, GreenEnergyPlatformDataSourceAddress(rewarded = false))
          }
          val carbonFootprintCalculatorAddresses = registeredWallets.foldLeft(Map.empty[Address, CarbonFootprintCalculatorDataSourceAddress]) { (acc, address) =>
            acc.updated(address, CarbonFootprintCalculatorDataSourceAddress(rewarded = false))
          }
          val investmentPlatformAddresses = registeredWallets.foldLeft(Map.empty[Address, InvestmentPlatformDataSourceAddress]) { (acc, address) =>
            acc.updated(address, InvestmentPlatformDataSourceAddress(rewarded = false))
          }
          (greenEnergyPlatformAddresses, carbonFootprintCalculatorAddresses, investmentPlatformAddresses)
        }

        override def genesis: DataState[EnvironetOnChainState, EnvironetCalculatedState] = {
          val registeredWalletsAsString = readLinesFromFile("registered_wallets.txt")
          val registeredWalletsList = registeredWalletsAsString.flatMap { addressAsString =>
            refineV[DAGAddressRefined](addressAsString).toOption.map(Address(_))
          }

          val (greenEnergyPlatformAddresses, carbonFootprintCalculatorAddresses, investmentPlatformAddresses) = buildDataSourceAddresses(registeredWalletsList)

          DataState(
            EnvironetOnChainState(List.empty),
            EnvironetCalculatedState(Map(
              DataSourceType.GreenEnergyPlatform -> GreenEnergyPlatformDataSource(greenEnergyPlatformAddresses),
              DataSourceType.CarbonFootprintCalculator -> CarbonFootprintCalculatorDataSource(carbonFootprintCalculatorAddresses),
              DataSourceType.InvestmentPlatform -> InvestmentPlatformDataSource(investmentPlatformAddresses),
              DataSourceType.ManualSubmission -> ManualSubmissionDataSource(Map.empty)
            ))
          )
        }

        override def validateUpdate(
          update: EnvironetUpdate
        )(implicit context: L0NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
          valid.pure

        override def validateData(
          state  : DataState[EnvironetOnChainState, EnvironetCalculatedState],
          updates: NonEmptyList[Signed[EnvironetUpdate]]
        )(implicit context: L0NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
          valid.pure

        override def combine(
          state  : DataState[EnvironetOnChainState, EnvironetCalculatedState],
          updates: List[Signed[EnvironetUpdate]]
        )(implicit context: L0NodeContext[F]): F[DataState[EnvironetOnChainState, EnvironetCalculatedState]] =
          LifecycleSharedFunctions.combine[F](
            state,
            updates,
            applicationConfig
          )

        override def dataEncoder: Encoder[EnvironetUpdate] =
          implicitly[Encoder[EnvironetUpdate]]

        override def calculatedStateEncoder: Encoder[EnvironetCalculatedState] =
          implicitly[Encoder[EnvironetCalculatedState]]

        override def dataDecoder: Decoder[EnvironetUpdate] =
          implicitly[Decoder[EnvironetUpdate]]

        override def calculatedStateDecoder: Decoder[EnvironetCalculatedState] =
          implicitly[Decoder[EnvironetCalculatedState]]

        override def signedDataEntityDecoder: EntityDecoder[F, Signed[EnvironetUpdate]] =
          circeEntityDecoder

        override def serializeBlock(
          block: Signed[DataApplicationBlock]
        ): F[Array[Byte]] =
          JsonSerializer[F].serialize[Signed[DataApplicationBlock]](block)

        override def deserializeBlock(
          bytes: Array[Byte]
        ): F[Either[Throwable, Signed[DataApplicationBlock]]] =
          JsonSerializer[F].deserialize[Signed[DataApplicationBlock]](bytes)

        override def serializeState(
          state: EnvironetOnChainState
        ): F[Array[Byte]] =
          JsonSerializer[F].serialize[EnvironetOnChainState](state)

        override def deserializeState(
          bytes: Array[Byte]
        ): F[Either[Throwable, EnvironetOnChainState]] =
          JsonSerializer[F].deserialize[EnvironetOnChainState](bytes)
      },
      applicationConfig
    )
}
