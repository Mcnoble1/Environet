package org.environet.data_l1

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all._
import io.circe.{Decoder, Encoder}
import org.environet.shared_data.LifecycleSharedFunctions
import org.environet.shared_data.calculated_state.CalculatedStateService
import org.environet.shared_data.types.DataUpdates._
import org.environet.shared_data.types.States._
import org.environet.shared_data.types.codecs.DataUpdateCodec._
import org.environet.shared_data.validations.Errors.valid
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.json.JsonSerializer
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

object DataL1Service {

  def make[F[+_] : Async : JsonSerializer](): F[BaseDataApplicationL1Service[F]] =
    for {
      calculatedStateService <- CalculatedStateService.make[F]
      dataApplicationL1Service = makeBaseDataApplicationL1Service(
        calculatedStateService
      )
    } yield dataApplicationL1Service

  private def makeBaseDataApplicationL1Service[F[+_] : Async : JsonSerializer](
    calculatedStateService: CalculatedStateService[F]
  ): BaseDataApplicationL1Service[F] = BaseDataApplicationL1Service(
    new DataApplicationL1Service[F, EnvironetUpdate, EnvironetOnChainState, EnvironetCalculatedState] {
      override def validateData(
        state  : DataState[EnvironetOnChainState, EnvironetCalculatedState],
        updates: NonEmptyList[Signed[EnvironetUpdate]]
      )(implicit context: L1NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
        valid.pure

      override def validateUpdate(
        update: EnvironetUpdate
      )(implicit context: L1NodeContext[F]): F[DataApplicationValidationErrorOr[Unit]] =
        LifecycleSharedFunctions.validateUpdate(update)

      override def combine(
        state  : DataState[EnvironetOnChainState, EnvironetCalculatedState],
        updates: List[Signed[EnvironetUpdate]]
      )(implicit context: L1NodeContext[F]): F[DataState[EnvironetOnChainState, EnvironetCalculatedState]] =
        state.pure

      override def routes(implicit context: L1NodeContext[F]): HttpRoutes[F] =
        HttpRoutes.empty

      override def dataEncoder: Encoder[EnvironetUpdate] =
        implicitly[Encoder[EnvironetUpdate]]

      override def dataDecoder: Decoder[EnvironetUpdate] =
        implicitly[Decoder[EnvironetUpdate]]

      override def calculatedStateEncoder: Encoder[EnvironetCalculatedState] =
        implicitly[Encoder[EnvironetCalculatedState]]

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

      override def serializeUpdate(
        update: EnvironetUpdate
      ): F[Array[Byte]] =
        JsonSerializer[F].serialize[EnvironetUpdate](update)

      override def deserializeUpdate(
        bytes: Array[Byte]
      ): F[Either[Throwable, EnvironetUpdate]] =
        JsonSerializer[F].deserialize[EnvironetUpdate](bytes)

      override def getCalculatedState(implicit context: L1NodeContext[F]): F[(SnapshotOrdinal, EnvironetCalculatedState)] =
        calculatedStateService.get.map(calculatedState => (calculatedState.ordinal, calculatedState.state))

      override def setCalculatedState(
        ordinal: SnapshotOrdinal,
        state  : EnvironetCalculatedState
      )(implicit context: L1NodeContext[F]): F[Boolean] =
        calculatedStateService.update(ordinal, state)

      override def hashCalculatedState(
        state: EnvironetCalculatedState
      )(implicit context: L1NodeContext[F]): F[Hash] =
        calculatedStateService.hash(state)

      override def serializeCalculatedState(
        state: EnvironetCalculatedState
      ): F[Array[Byte]] =
        JsonSerializer[F].serialize[EnvironetCalculatedState](state)

      override def deserializeCalculatedState(
        bytes: Array[Byte]
      ): F[Either[Throwable, EnvironetCalculatedState]] =
        JsonSerializer[F].deserialize[EnvironetCalculatedState](bytes)
    }
  )
}
