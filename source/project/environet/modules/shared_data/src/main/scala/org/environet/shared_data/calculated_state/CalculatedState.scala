package org.environet.shared_data.calculated_state

import org.environet.shared_data.types.States.EnvironetCalculatedState
import org.tessellation.schema.SnapshotOrdinal

case class CalculatedState(ordinal: SnapshotOrdinal, state: EnvironetCalculatedState)

object CalculatedState {
  def empty: CalculatedState =
    CalculatedState(SnapshotOrdinal.MinValue, EnvironetCalculatedState(Map.empty))
}
