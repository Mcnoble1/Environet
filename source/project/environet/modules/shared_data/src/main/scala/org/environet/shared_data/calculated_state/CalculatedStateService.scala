package org.environet.shared_data.calculated_state

import cats.effect.Ref
import cats.effect.kernel.Async
import cats.syntax.all._
import org.environet.shared_data.types.States.EnvironetCalculatedState
import org.tessellation.json.JsonSerializer
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.hash.Hash

trait CalculatedStateService[F[_]] {
  def get: F[CalculatedState]

  def update(
    snapshotOrdinal: SnapshotOrdinal,
    state          : EnvironetCalculatedState
  ): F[Boolean]

  def hash(
    state: EnvironetCalculatedState
  ): F[Hash]
}

object CalculatedStateService {
  def make[F[_] : Async : JsonSerializer]: F[CalculatedStateService[F]] = {
    Ref.of[F, CalculatedState](CalculatedState.empty).map { stateRef =>
      new CalculatedStateService[F] {
        override def get: F[CalculatedState] = stateRef.get

        override def update(
          snapshotOrdinal: SnapshotOrdinal,
          state          : EnvironetCalculatedState
        ): F[Boolean] =
          stateRef.update { currentState =>
            val currentCalculatedState = currentState.state
            val updatedDevices = state.dataSources.foldLeft(currentCalculatedState.dataSources) {
              case (acc, (address, value)) =>
                acc.updated(address, value)
            }

            CalculatedState(snapshotOrdinal, EnvironetCalculatedState(updatedDevices))
          }.as(true)

        override def hash(
          state: EnvironetCalculatedState
        ): F[Hash] = JsonSerializer[F].serialize[EnvironetCalculatedState](state).map(Hash.fromBytes)
      }
    }
  }
}
